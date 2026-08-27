(ns examples.files
  "Use the beta Files API helpers.")

(require '[anthropic.core :as anthropic])

(comment
  (def client (anthropic/client))

  ;; Replace this placeholder with a real local path before evaluating. The
  ;; wrapper accepts a path string, java.io.File, Path, InputStream, or bytes.
  ;; Files expire according to the API's retention policy.
  (def uploaded
    (anthropic/upload-file client "path/to/document.pdf"))

  ;; Metadata and the paginated collection are returned as ordinary maps.
  (anthropic/get-file client (:id uploaded))
  (anthropic/list-files client)

  ;; Delete the example upload when it is no longer needed. This is an explicit
  ;; API operation, so evaluate it only when you intend to remove that file.
  (anthropic/delete-file client (:id uploaded)))
