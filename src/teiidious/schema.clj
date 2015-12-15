(ns teiidious.schema
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc])
  (:import [graphql.schema GraphQLObjectType GraphQLSchema GraphQLFieldDefinition
            DataFetcher DataFetchingEnvironment GraphQLNonNull GraphQLList GraphQLArgument]
           [graphql GraphQL Scalars]))

(def hello
  (-> (GraphQLObjectType/newObject)
    (.name "helloWorldQuery")
    (.field (-> (GraphQLFieldDefinition/newFieldDefinition)
              (.type Scalars/GraphQLString)
              (.name "hello")
              (.staticValue "world")
              .build))
    .build))

(defn field-type
  "SQL to GraphQL type mapping"
  [m]
  (case (:type_name m)
    "integer"    Scalars/GraphQLInt
    ;; TODO: define scalar for "bigdecimal" Scalars/GraphQLFloat
    "boolean"    Scalars/GraphQLBoolean
    Scalars/GraphQLString))

(defn check-required
  [type m]
  (if (= 0 (:nullable m))
    (GraphQLNonNull. type)
    type))

(defn column->field [m]
  (-> (GraphQLFieldDefinition/newFieldDefinition)
    (.type (check-required (field-type m) m))
    (.name (str/lower-case (:column_name m)))
    (.build)))

(defn column->argument [m]
  (-> (GraphQLArgument/newArgument)
    (.type (field-type m))
    (.name (str/lower-case (:column_name m)))
    (.build)))

(defn columns
  "Expects table name and db connection spec"
  [db-spec table]
  (jdbc/with-db-metadata [md db-spec]
    (jdbc/metadata-query (.getColumns md nil nil table nil))))

(defn table->object
  [table columns]
  (-> (GraphQLObjectType/newObject)
    (.name table)
    (.fields (for [m columns] (column->field m)))
    (.build)))

(defn table->field
  [db-spec table]
  (let [columns (columns db-spec table)]
    (-> (GraphQLFieldDefinition/newFieldDefinition)
      (.name (str/lower-case table))
      (.type (GraphQLList. (table->object table columns)))
      (.argument (map column->argument columns))
      (.dataFetcher
        (reify DataFetcher
          (get [_ ^DataFetchingEnvironment env]
            (let [args (into {} (remove (comp nil? second) (into {} (.getArguments env))))
                  stmt (if (empty? args)
                         (str "select * from " table)
                         (str "select * from " table " where "
                           (str/join " and " (map #(str % " = ?") (keys args)))))]
              (-> (jdbc/query db-spec (cons stmt (vals args)))
                clojure.walk/stringify-keys)))))
      (.build))))

(defn query
  [name & fields]
  (-> (GraphQLObjectType/newObject)
    (.name name)
    (.fields fields)
    (.build)))

(defn schema
  [query-type]
  (-> (GraphQLSchema/newSchema)
    (.query query-type)
    .build))
