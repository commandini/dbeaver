/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.runtime.ui;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Connections Service
 */
public interface UIServiceConnections {

    void openConnectionEditor(@NotNull DBPDataSourceContainer dataSourceContainer, @Nullable String defaultPageName);

    void connectDataSource(@NotNull DBPDataSourceContainer dataSourceContainer, DBRProgressListener onFinish);

    void disconnectDataSource(@NotNull DBPDataSourceContainer dataSourceContainer);

    void closeActiveTransaction(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, boolean commitTxn);

    void confirmTransactionsClose(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, boolean commitTxn);

    boolean checkAndCloseActiveTransaction(@NotNull DBCExecutionContext[] contexts);

}