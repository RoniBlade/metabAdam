(ns metabase-enterprise.audit-app.pages.common.card-and-dashboard-detail
  "Common queries used by both Card (Question) and Dashboard detail pages."
  (:require
   [metabase-enterprise.audit-app.pages.common :as common]
   [metabase.models.card :refer [Card]]
   [metabase.models.dashboard :refer [Dashboard]]
   [metabase.models.revision :as revision]
   [metabase.util.honey-sql-2 :as h2x]
   [metabase.util.malli :as mu]
   [metabase.util.malli.schema :as ms]))

(def ^:private ModelName
  [:enum "card" "dashboard"])

;; SELECT {{group-fn(timestamp}} AS "date", count(*) AS views
;; FROM view_log
;; WHERE model = {{model}}
;;   AND model_id = {{model-id}}
;; GROUP BY {{group-fn(timestamp}}
;; ORDER BY {{group-fn(timestamp}} ASC
(mu/defn views-by-time
  "Get views of a Card or Dashboard broken out by a time `unit`, e.g. `day` or `day-of-week`."
  [model    :- ModelName
   model-id :- ms/PositiveInt
   unit     :- common/DateTimeUnitStr]
  {:metadata [[:date  {:display_name "Date",  :base_type (common/datetime-unit-str->base-type unit)}]
              [:views {:display_name "Views", :base_type :type/Integer}]]
   :results (let [grouped-timestamp (common/grouped-datetime unit :timestamp)]
              (common/reducible-query
                (-> {:select   [[grouped-timestamp :date]
                                [:%count.* :views]]
                     :from     [:view_log]
                     :where    [:and
                                [:= :model (h2x/literal model)]
                                [:= :model_id model-id]]
                     :group-by [grouped-timestamp]
                     :order-by [[grouped-timestamp :asc]]}
                    (common/add-45-days-clause :timestamp))))})

(mu/defn cached-views-by-time
  "Get number of views of a Card broken out by a time `unit`, e.g. `day` or `day-of-week` and by cache status.
  Still here instead of in cards because of similarity to views-by-time"
  [card-id :- ms/PositiveInt
   unit    :- common/DateTimeUnitStr]
  {:metadata [[:date           {:display_name "Date",
                                :base_type (common/datetime-unit-str->base-type unit)}]
              [:cached-views   {:display_name "Cached Views",
                                :base_type :type/Integer}]
              [:uncached-views {:display_name "Uncached Views",
                                :base_type :type/Integer}]]
   :results (let [grouped-timestamp (common/grouped-datetime unit :started_at)]
              (common/reducible-query
                (->
                  {:select    [[grouped-timestamp :date]
                               [[:sum [:case [:= :cache_hit true]  [:inline 1] :else [:inline 0]]] :cached_views]
                               [[:sum [:case [:= :cache_hit false] [:inline 1] :else [:inline 0]]] :uncached_views]]
                   :from      [:query_execution]
                   :where     [:and
                               [:= :card_id card-id]
                               [:not= :cache_hit nil]]
                   :group-by  [grouped-timestamp]
                   :order-by  [[grouped-timestamp :asc]]}
                  (common/add-45-days-clause :started_at))))})

(mu/defn avg-execution-time-by-time
  "Get average execution time of a Card broken out by a time `unit`, e.g. `day` or `day-of-week`.
  Still here instead of in cards because of similarity to views-by-time"
  [card-id :- ms/PositiveInt
   unit    :- common/DateTimeUnitStr]
  {:metadata [[:date        {:display_name "Date",            :base_type (common/datetime-unit-str->base-type unit)}]
              [:avg_runtime {:display_name "Average Runtime", :base_type :type/Number}]]
   :results (let [grouped-timestamp (common/grouped-datetime unit :started_at)]
              (common/reducible-query
                (-> {:select   [[grouped-timestamp :date]
                                [[:avg :running_time] :avg_runtime]]
                     :from     [:query_execution]
                     :where    [:= :card_id card-id]
                     :group-by [grouped-timestamp]
                     :order-by [[grouped-timestamp :asc]]}
                    (common/add-45-days-clause :started_at))))})

(mu/defn revision-history
  "Get a revision history table for a Card or Dashboard."
  [model    :- [:enum Card Dashboard]
   model-id :- ms/PositiveInt]
  {:metadata [[:timestamp   {:display_name "Edited on",   :base_type :type/DateTime}]
              [:user_id     {:display_name "User ID",     :base_type :type/Integer, :remapped_to   :user_name}]
              [:user_name   {:display_name "Edited by",   :base_type :type/Name,    :remapped_from :user_id}]
              [:change_made {:display_name "Change made", :base_type :type/Text}]
              [:revision_id {:display_name "Revision ID", :base_type :type/Integer}]]
   :results (for [revision (revision/revisions+details model model-id)]
              {:timestamp   (-> revision :timestamp)
               :user_id     (-> revision :user :id)
               :user_name   (-> revision :user :common_name)
               :change_made (-> revision :description)
               :revision_id (-> revision :id)})})

(mu/defn audit-log
  "Get a view log for a Card or Dashboard."
  [model    :- ModelName
   model-id :- ms/PositiveInt]
  {:metadata [[:when    {:display_name "When",    :base_type :type/DateTime}]
              [:user_id {:display_name "User ID", :base_type :type/Integer, :remapped_to   :who}]
              [:who     {:display_name "Who",     :base_type :type/Name,    :remapped_from :user_id}]
              [:what    {:display_name "What",    :base_type :type/Text}]]
   :results (common/reducible-query
              {:select    [[:vl.timestamp :when]
                           :vl.user_id
                           [(common/user-full-name :u) :who]
                           [:vl.metadata :what]]
               :from      [[:view_log :vl]]
               :join     [[:core_user :u] [:= :vl.user_id :u.id]]
               :where     [:and
                           [:= :model (h2x/literal model)]
                           [:= :model_id model-id]]
               :order-by  [[:vl.timestamp :desc]
                           [[:lower :u.last_name] :asc]
                           [[:lower :u.first_name] :asc]]})})
