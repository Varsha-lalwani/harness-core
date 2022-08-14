// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"context"
	"fmt"
	"io"
	"os"
	"time"

	"github.com/harness/harness-core/commons/go/lib/exec"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/images"
	"github.com/harness/harness-core/commons/go/lib/utils"
	pb "github.com/harness/harness-core/product/ci/engine/proto"
	"go.uber.org/zap"
)

// BackgroundTask represents interface to execute a background step
type BackgroundTask interface {
	Run() (map[string]string, int32, error)
}

type backgroundTask struct {
	id                string
	displayName       string
	command           string
	shellType         pb.ShellType
	environment       map[string]string
<<<<<<< HEAD
=======
	timeoutSecs       int64
	numRetries        int32
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
	reports           []*pb.Report
	logMetrics        bool
	log               *zap.SugaredLogger
	addonLogger       *zap.SugaredLogger
	procWriter        io.Writer
	fs                filesystem.FileSystem
<<<<<<< HEAD
	cmdFactory        exec.CommandFactory
=======
	cmdContextFactory exec.CmdContextFactory
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
	entrypoint        []string
	image			  string
}

// NewBackgroundTask creates a background step executor
func NewBackgroundTask(step *pb.UnitStep, log *zap.SugaredLogger, w io.Writer, logMetrics bool, 
	addonLogger *zap.SugaredLogger) BackgroundTask {
	r := step.GetRun()
	fs := filesystem.NewOSFileSystem(log)
<<<<<<< HEAD
=======

	timeoutSecs := r.GetContext().GetExecutionTimeoutSecs()
	if timeoutSecs == 0 {
		timeoutSecs = defaultTimeoutSecs
	}

	numRetries := r.GetContext().GetNumRetries()
	if numRetries == 0 {
		numRetries = defaultNumRetries
	}
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
	
	return &backgroundTask{
		id:                step.GetId(),
		displayName:       step.GetDisplayName(),
		command:           r.GetCommand(),
		shellType:         r.GetShellType(),
		environment:       r.GetEnvironment(),
		reports:           r.GetReports(),
<<<<<<< HEAD
		cmdFactory:        exec.OsCommand(),
=======
		timeoutSecs:       timeoutSecs,
		numRetries:        numRetries,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
		logMetrics:        logMetrics,
		log:               log,
		fs:                fs,
		procWriter:        w,
		addonLogger:       addonLogger,
		entrypoint:		   r.GetEntrypoint(),
		image:			   r.GetImage(),
	}
}

// Executes customer provided background step command with retries and timeout handling
func (b *backgroundTask) Run() (map[string]string, int32, error) {
	ch := make(chan error, 1)
	resp, retries, err := b.runAsync(ch)
	
	select {
	case ret := <-ch:
		err = ret
	case <-time.After(5 * time.Second):
		err = nil
	}

	return resp, retries, err
}

func (b* backgroundTask) runAsync(ch chan error) (map[string]string, int32, error) {
	go func() {
		var (
			err error
		)
	
		ctx := context.Background()
<<<<<<< HEAD
		
		if _, err = b.execute(ctx, defaultNumRetries); err == nil {
			st := time.Now()
			err = collectTestReports(ctx, b.reports, b.id, b.log)
			if err != nil {
				// If there's an error in collecting reports, we won't retry but
				// the step will be marked as an error
				b.log.Errorw("unable to collect test reports", zap.Error(err))
				ch <- err
			} else if len(b.reports) > 0 {
				b.log.Infow(fmt.Sprintf("collected test reports in %s time", time.Since(st)))
			}
		}

=======
	
		for i := int32(1); i <= b.numRetries; i++ {
			if _, err = b.execute(ctx, i); err == nil {
				st := time.Now()
				err = collectTestReports(ctx, b.reports, b.id, b.log)
				if err != nil {
					// If there's an error in collecting reports, we won't retry but
					// the step will be marked as an error
					b.log.Errorw("unable to collect test reports", zap.Error(err))
					ch <- err
				} else if len(b.reports) > 0 {
					b.log.Infow(fmt.Sprintf("collected test reports in %s time", time.Since(st)))
				}
			}
		}
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
		if err != nil {
			// Background step did not execute successfully
			// Try and collect reports, ignore any errors during report collection itself
			err = collectTestReports(ctx, b.reports, b.id, b.log)
			if err != nil {
				b.log.Errorw("error while collecting test reports", zap.Error(err))
			}
		}

		ch <- err
	}()

<<<<<<< HEAD
	return make(map[string]string), defaultNumRetries, nil
=======
	return make(map[string]string), b.numRetries, nil
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
}

func (b *backgroundTask) execute(ctx context.Context, retryCount int32) (map[string]string, error) {
	start := time.Now()
<<<<<<< HEAD
=======
	// ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(b.timeoutSecs))
	// defer cancel()
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)

	cmdToExecute, err := b.getScript(ctx)
	if err != nil {
		return nil, err
	}

	envVars, err := resolveExprInEnv(b.environment)
	if err != nil {
		return nil, err
	}

	cmdArgs, err := b.getEntrypoint(ctx, cmdToExecute)
	if err != nil {
		return nil, err
	}

<<<<<<< HEAD
	cmd := b.cmdFactory.Command(cmdArgs[0], cmdArgs[1:]...).
=======
	cmd := b.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, cmdArgs[0], cmdArgs[1:]...).
>>>>>>> 67197f146f5 ([CI-5075]: LE and Addon changes for background step)
		WithStdout(b.procWriter).WithStderr(b.procWriter).WithEnvVarsMap(envVars)
	err = runCmd(ctx, cmd, b.id, cmdArgs, retryCount, start, b.logMetrics, b.addonLogger)
	if err != nil {
		return nil, err
	}

	stepOutput := make(map[string]string)

	b.addonLogger.Infow(
		"Successfully executed background step",
		"arguments", cmdArgs,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return stepOutput, nil
}

func (b *backgroundTask) getScript(ctx context.Context) (string, error) {
	if len(b.command) == 0 {
		return "", nil
	}

	resolvedCmd, err := resolveExprInCmd(b.command)
	if err != nil {
		return "", err
	}

	earlyExitCmd, err := b.getEarlyExitCommand()
	if err != nil {
		return "", err
	}

	// Using set -xe instead of printing command via utils.GetLoggableCmd(command) since if ' is present in a command,
	// echo on the command fails with an error.
	command := fmt.Sprintf("%s%s", earlyExitCmd, resolvedCmd)
	return command, nil
}

func (b *backgroundTask) getShell(ctx context.Context) (string, string, error) {
	if b.shellType == pb.ShellType_BASH {
		return "bash", "-c", nil
	} else if b.shellType == pb.ShellType_SH {
		return "sh", "-c", nil
	} else if b.shellType == pb.ShellType_POWERSHELL {
		return "powershell", "-Command", nil
	} else if b.shellType == pb.ShellType_PWSH {
		return "pwsh", "-Command", nil
	}
	return "", "", fmt.Errorf("Unknown shell type: %s", b.shellType)
}

func (b *backgroundTask) getEntrypoint(ctx context.Context, cmdToExecute string) ([]string, error) {
	// give priority to entrypoint
	if len(b.entrypoint) != 0 {
		return b.entrypoint, nil
	} else if len(cmdToExecute) != 0 {
		shell, ep, err := b.getShell(ctx)
		if err != nil {
			return nil, err
		}
		return []string{shell, ep, cmdToExecute}, nil
	}

	// fetch default ep
	imageSecret, _ := os.LookupEnv(imageSecretEnv)
	ep, args, err := getImgMetadata(ctx, b.id, b.image, imageSecret, b.log)
	if err != nil {
		return nil, err
	}

	return images.CombinedEntrypoint(ep, args), nil
}

func (b *backgroundTask) getEarlyExitCommand() (string, error) {
	if b.shellType == pb.ShellType_BASH || b.shellType == pb.ShellType_SH {
		return "set -xe\n", nil
	} else if b.shellType == pb.ShellType_POWERSHELL || b.shellType == pb.ShellType_PWSH {
		return "$ErrorActionPreference = 'Stop' \n", nil
	}
	return "", fmt.Errorf("Unknown shell type: %s", b.shellType)
}
