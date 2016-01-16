(ns gridfire.fuel-models-test
  (:require [clojure.test :refer :all]
            [gridfire.fuel-models :refer :all]
            [gridfire.behaveplus-results :refer :all]))

;; Checks live fuel moisture of extinction and dynamic fuel loading for the S&B40 fuel models under fully cured conditions
(deftest moisturize-test-dry
  (doseq [num sb40-fuel-models]
    (let [gridfire-fuel-model-dry   (build-fuel-model num)
          gridfire-fuel-model-wet   (moisturize gridfire-fuel-model-dry (test-fuel-moisture :dry))
          gridfire-M_x-live         (* 100 (-> gridfire-fuel-model-wet :M_x :live :herbaceous))
          gridfire-fraction-cured   (if (pos? (-> gridfire-fuel-model-dry :w_o :live :herbaceous))
                                      (* 100 (/ (-> gridfire-fuel-model-wet :w_o :dead :herbaceous)
                                                (-> gridfire-fuel-model-dry :w_o :live :herbaceous)))
                                      0)
          behaveplus-outputs        (-> (:name gridfire-fuel-model-dry)
                                        (behaveplus5-surface-fire-values-dry-no-wind-no-slope))
          behaveplus-M_x-live       (behaveplus-outputs 4)
          behaveplus-fraction-cured (behaveplus-outputs 6)]
      (is (within gridfire-M_x-live behaveplus-M_x-live 6)) ;; all are within 1% except TU2 at -6%
      (is (within gridfire-fraction-cured behaveplus-fraction-cured 0.1)))))

;; Checks live fuel moisture of extinction and dynamic fuel loading for the S&B40 fuel models under 50% cured conditions
(deftest moisturize-test-mid
  (doseq [num sb40-fuel-models]
    (let [gridfire-fuel-model-dry   (build-fuel-model num)
          gridfire-fuel-model-wet   (moisturize gridfire-fuel-model-dry (test-fuel-moisture :mid))
          gridfire-M_x-live         (* 100 (-> gridfire-fuel-model-wet :M_x :live :herbaceous))
          gridfire-fraction-cured   (if (pos? (-> gridfire-fuel-model-dry :w_o :live :herbaceous))
                                      (* 100 (/ (-> gridfire-fuel-model-wet :w_o :dead :herbaceous)
                                                (-> gridfire-fuel-model-dry :w_o :live :herbaceous)))
                                      0)
          behaveplus-outputs        (-> (:name gridfire-fuel-model-dry)
                                        (behaveplus5-surface-fire-values-mid-no-wind-no-slope))
          behaveplus-M_x-live       (behaveplus-outputs 4)
          behaveplus-fraction-cured (behaveplus-outputs 6)]
      (is (within gridfire-M_x-live behaveplus-M_x-live 6)) ;; all are within 1% except TU2 at -6%
      (is (within gridfire-fraction-cured behaveplus-fraction-cured 0.1)))))

;; TODO: Add moisturize-test-wet
