/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TestTopicQueuableObjectListener extends QueueListener<TestTopicQueuableObject> {
  private boolean throwException;

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  @Inject
  TestTopicQueuableObjectListener(QueueConsumer<TestTopicQueuableObject> consumer) {
    super(consumer, true);
  }

  @Override
  public void onMessage(TestTopicQueuableObject message) {
    if (throwException) {
      throw new RuntimeException("Expected Exception In Test.");
    }
  }
}
