// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"bytes"
	"context"
	"testing"
	"os/exec"

	"github.com/golang/mock/gomock"
	mexec "github.com/harness/harness-core/commons/go/lib/exec"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/logs"
	pb "github.com/harness/harness-core/product/ci/engine/proto"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
)

func TestBackgroundTaskCreate(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:     "redis-server",
				Detach:      true,
				Image:       "redis",
				Entrypoint:  []string{"redis-server", "--loglevel", "debug"},
			},
		},
	}

	var buf bytes.Buffer
	executor := NewBackgroundTask(step, log.Sugar(), &buf, false, log.Sugar())
	assert.NotNil(t, executor)
}

func TestBackgroundExecuteSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCommandFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	pstate := mexec.NewMockProcessState(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	entrypoint := []string{"git", "status"}
	b := &backgroundTask{
		id:                "step1",
		logMetrics:        true,
		log:               log.Sugar(),
		addonLogger:       log.Sugar(),
		fs:                fs,
		cmdFactory:        cmdFactory,
		procWriter:        &buf,
		entrypoint:        entrypoint,
		image:			   "plugin/drone-git",
	}

	oldMlog := mlog
	mlog = func(pid int32, id string, l *zap.SugaredLogger) {
		return
	}
	defer func() { mlog = oldMlog }()

	cmdFactory.EXPECT().Command(entrypoint[0], entrypoint[1]).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)
	cmd.EXPECT().Start().Return(nil)
	cmd.EXPECT().Pid().Return(int(1))
	cmd.EXPECT().ProcessState().Return(pstate)
	pstate.EXPECT().MaxRss().Return(int64(100), nil)
	cmd.EXPECT().Wait().Return(nil)

	o, retries, err := b.Run()
	assert.Nil(t, err)
	assert.Equal(t, len(o), 0)
	assert.Equal(t, retries, int32(1))
}

func TestBackgroundExecuteNonZeroStatus(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer
	entrypoint := []string{"git", "status"}
	svcID := "git-clone"
	image := "alpine/git"

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	cmdFactory := mexec.NewMockCommandFactory(ctrl)
	pstate := mexec.NewMockProcessState(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	
	b := &backgroundTask{
		id:          svcID,
		image:       image,
		logMetrics:  false,
		entrypoint:  entrypoint,
		log:         log.Sugar(),
		addonLogger: log.Sugar(),
		procWriter:  &buf,
		fs:          fs,
		cmdFactory:  cmdFactory,
	}

	cmdFactory.EXPECT().Command(entrypoint[0], entrypoint[1]).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)
	cmd.EXPECT().Start().Return(nil)
	cmd.EXPECT().Pid().Return(int(1))
	cmd.EXPECT().ProcessState().Return(pstate)
	pstate.EXPECT().MaxRss().Return(int64(100), nil)
	cmd.EXPECT().Wait().Return(&exec.ExitError{})

	_, _, err := b.Run()
	assert.NotNil(t, err)
	if _, ok := err.(*exec.ExitError); !ok {
		t.Fatalf("Expected err of type exec.ExitError")
	}
}