(ns gridfire.cli
  (:gen-class)
  (:require [clojure.core.matrix :as m]
            [gridfire.postgis-bridge :refer [postgis-raster-to-matrix]]
            [gridfire.surface-fire :refer [degrees-to-radians]]
            [gridfire.fire-spread :refer [run-fire-spread]]
            [matrix-viz.core :refer [save-matrix-as-png]]
            [magellan.core :refer [register-new-crs-definitions-from-properties-file!
                                   make-envelope matrix-to-raster write-raster]]))

(m/set-current-implementation :vectorz)

(register-new-crs-definitions-from-properties-file!
 "CALFIRE" "custom_projections.properties")

(defn fetch-landfire-layers
  "Returns a map of LANDFIRE rasters with the following units:
   {:elevation          feet
    :slope              vertical feet/horizontal feet
    :aspect             degrees clockwise from north
    :fuel-model         fuel model numbers 1-256
    :canopy-height      feet
    :canopy-base-height feet
    :crown-bulk-density lb/ft^3
    :canopy-cover       % (0-100)}"
  [db-spec layer->table]
  (let [landfire-layers (reduce (fn [amap layer]
                                  (let [table (layer->table layer)]
                                    (assoc amap layer
                                           (postgis-raster-to-matrix db-spec table))))
                                {}
                                [:elevation
                                 :slope
                                 :aspect
                                 :fuel-model
                                 :canopy-height
                                 :canopy-base-height
                                 :crown-bulk-density
                                 :canopy-cover])]
    (-> landfire-layers
        (update-in [:elevation :matrix]
                   (fn [matrix] (m/emap #(* % 3.28) matrix))) ; m -> ft
        (update-in [:slope :matrix]
                   (fn [matrix] (m/emap #(Math/tan (degrees-to-radians %)) matrix)))
                   ; degrees -> %
        (update-in [:canopy-height :matrix]
                   (fn [matrix] (m/emap #(* % 3.28) matrix))) ; m -> ft
        (update-in [:canopy-base-height :matrix]
                   (fn [matrix] (m/emap #(* % 3.28) matrix))) ; m -> ft
        (update-in [:crown-bulk-density :matrix]
                   (fn [matrix] (m/emap #(* % 0.0624) matrix)))))) ; kg/m^3 -> lb/ft^3

(defn -main
  [config-file]
  (let [config              (read-string (slurp config-file))
        landfire-layers     (fetch-landfire-layers (:db-spec config)
                                                   (:landfire-layers config))
        landfire-rasters    (into {}
                                  (map (fn [[layer info]] [layer (:matrix info)]))
                                  landfire-layers)
        fire-spread-results (run-fire-spread (:max-runtime               config)
                                             (:cell-size                 config)
                                             landfire-rasters
                                             (:wind-speed-20ft           config)
                                             (:wind-from-direction       config)
                                             (:fuel-moisture             config)
                                             (:foliar-moisture           config)
                                             (:ellipse-adjustment-factor config)
                                             (:ignition-site             config))
        envelope            (let [{:keys [upperleftx upperlefty width height scalex scaley]}
                                  (landfire-layers :elevation)]
                              (make-envelope (:srid config)
                                             upperleftx
                                             (+ upperlefty (* height scaley))
                                             (* width scalex)
                                             (* -1.0 height scaley)))]
    (when (:output-landfire-inputs? config)
      (doseq [[layer info] landfire-layers]
        (-> (matrix-to-raster (name layer) (:matrix info) envelope)
            (write-raster (str (name layer) (:outfile-suffix config) ".tif")))))
    (when (:output-geotiffs? config)
      (doseq [[name layer] [["fire_spread"         :fire-spread-matrix]
                            ["flame_length"        :flame-length-matrix]
                            ["fire_line_intensity" :fire-line-intensity-matrix]]]
        (-> (matrix-to-raster name (fire-spread-results layer) envelope)
            (write-raster (str name (:outfile-suffix config) ".tif")))))
    (when (:output-pngs? config)
      (doseq [[name layer] [["fire_spread"         :fire-spread-matrix]
                            ["flame_length"        :flame-length-matrix]
                            ["fire_line_intensity" :fire-line-intensity-matrix]]]
        (save-matrix-as-png :color 4 -1.0
                            (fire-spread-results layer)
                            (str name (:outfile-suffix config) ".png"))))
    (println "Global Clock:" (:global-clock fire-spread-results))
    (println "Ignited Cells:" (count (:ignited-cells fire-spread-results)))))
