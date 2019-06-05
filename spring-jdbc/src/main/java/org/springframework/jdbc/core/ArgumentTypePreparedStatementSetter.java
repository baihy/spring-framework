/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies
 * given arrays of arguments and JDBC argument types.
 *
 * @author Juergen Hoeller
 * @since 3.2.3
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

    @Nullable
    private final Object[] args;

    @Nullable
    private final int[] argTypes;


    /**
     * Create a new ArgTypePreparedStatementSetter for the given arguments.
     *
     * @param args     the arguments to set
     * @param argTypes the corresponding SQL types of the arguments
     */
    public ArgumentTypePreparedStatementSetter(@Nullable Object[] args, @Nullable int[] argTypes) {
        if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
                (args != null && args.length != argTypes.length)) {
            throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
        }
        this.args = args;
        this.argTypes = argTypes; // 传入的类型是jdbc的类型在Types类下
    }


    @Override  // 对预编译sql中的问号进行赋值 通过参数类型实现操作
    public void setValues(PreparedStatement ps) throws SQLException {
        int parameterPosition = 1;
        if (this.args != null && this.argTypes != null) {
            // 遍历每个参数以作为类型的匹配和转换
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                // 如果是集合，需要进入遍历集合的内部类型
                if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
                    Collection<?> entries = (Collection<?>) arg;
                    for (Object entry : entries) {
                        if (entry instanceof Object[]) {
                            Object[] valueArray = ((Object[]) entry);
                            for (Object argValue : valueArray) {
                                // 赋值的核心方法
                                doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
                                parameterPosition++;
                            }
                        } else {
                            // 赋值的核心方法
                            doSetValue(ps, parameterPosition, this.argTypes[i], entry);
                            parameterPosition++;
                        }
                    }
                } else {
                    // 赋值的核心方法
                    doSetValue(ps, parameterPosition, this.argTypes[i], arg);
                    parameterPosition++;
                }
            }
        }
    }

    /**
     * Set the value for the prepared statement's specified parameter position using the passed in
     * value and type. This method can be overridden by sub-classes if needed.
     *
     * @param ps                the PreparedStatement
     * @param parameterPosition index of the parameter position
     * @param argType           the argument type
     * @param argValue          the argument value
     * @throws SQLException if thrown by PreparedStatement methods
     */
    protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue)
            throws SQLException {
        // 为PreparedStatement参数赋值的核心方法
        StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
    }

    @Override
    public void cleanupParameters() {
        StatementCreatorUtils.cleanupParameters(this.args);
    }

}
