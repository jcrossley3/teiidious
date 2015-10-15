(ns teiidious.graphql
  (:import [graphql.schema GraphQLObjectType GraphQLSchema GraphQLFieldDefinition]
           [graphql GraphQL Scalars]))

(def hello-schema
  (let [type (-> (GraphQLObjectType/newObject)
               (.name "helloWorldQuery")
               (.field (-> (GraphQLFieldDefinition/newFieldDefinition)
                         (.type Scalars/GraphQLString)
                         (.name "hello")
                         (.staticValue "world")
                         .build))
               .build)]
    (-> (GraphQLSchema/newSchema)
      (.query type)
      .build)))

(defn execute
  [schema query]
  (-> (GraphQL. schema)
    (.execute query)
    .getData))

