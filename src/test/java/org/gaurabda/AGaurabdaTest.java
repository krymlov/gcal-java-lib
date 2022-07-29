/*
 * Copyright (C) By the Author
 * Author    Yura Krymlov
 * Created   2021-02
 */
package org.gaurabda;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * @author Yura Krymlov
 * @version 1.0, 2021-02
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class AGaurabdaTest {

}
