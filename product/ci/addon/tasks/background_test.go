// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"bytes"
	"testing"

	"github.com/harness/harness-core/commons/go/lib/logs"
	pb "github.com/harness/harness-core/product/ci/engine/proto"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
)

func TestRunTaskCreate(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:     "redis-server",
				Detach:      true,
				Image:       "alpine",
				Entrypoint:  []string{"redis-server", "--loglevel", "debug"},
			},
		},
	}

	var buf bytes.Buffer
	executor := NewBackgroundTask(step, log.Sugar(), &buf, false, log.Sugar())
	assert.NotNil(t, executor)
}