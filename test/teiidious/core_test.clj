(ns teiidious.core-test
  (:require [clojure.test :refer :all]
            [teiidious.core :refer :all]))

(defonce teiid-server (start-teiid))

(deftest simple-query
  (let [schema (create-schema teiid-server)]
    (is (= 17 (-> schema (.execute "{account {ssn}}") .getData (get "account") count)))))
