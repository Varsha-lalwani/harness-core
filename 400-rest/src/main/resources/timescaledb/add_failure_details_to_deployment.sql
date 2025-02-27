-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

BEGIN;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS FAILURE_DETAILS TEXT;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS FAILED_STEP_NAMES TEXT;
ALTER TABLE DEPLOYMENT ADD COLUMN IF NOT EXISTS FAILED_STEP_TYPES TEXT;

CREATE INDEX IF NOT EXISTS FAILURE_DETAILS_INDEX ON DEPLOYMENT(failure_details,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS FAILED_STEP_NAMES_INDEX ON DEPLOYMENT(failed_step_names,ENDTIME DESC);
CREATE INDEX IF NOT EXISTS FAILED_STEP_TYPES_INDEX ON DEPLOYMENT(failed_step_types,ENDTIME DESC);
COMMIT ;