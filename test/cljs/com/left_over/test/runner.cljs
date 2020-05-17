(ns com.left-over.test.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    com.left-over.api.handlers.auth-test
    com.left-over.api.services.db.models.core-test
    com.left-over.api.services.db.models.locations-test
    com.left-over.api.services.db.models.shows-test
    com.left-over.api.services.db.models.users-test
    com.left-over.api.services.dropbox-test
    com.left-over.api.services.google-test
    com.left-over.integration.tests.core
    com.left-over.ui.admin.services.store.reducers-test))

(doo-tests 'com.left-over.api.handlers.auth-test
           'com.left-over.api.services.db.models.core-test
           'com.left-over.api.services.db.models.locations-test
           'com.left-over.api.services.db.models.shows-test
           'com.left-over.api.services.db.models.users-test
           'com.left-over.api.services.dropbox-test
           'com.left-over.api.services.google-test
           'com.left-over.integration.tests.core
           'com.left-over.ui.admin.services.store.reducers-test)
