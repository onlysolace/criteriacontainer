/* 
 * Copyright ©2009 Jean-François Lamy
 * 
 * Licensed under the Open Software Licence, Version 3.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.opensource.org/licenses/osl-3.0.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.vaadin.addons.criteriacore;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

/**
 * Log an exception trace to the log file.
 * Useful to print a "where called trace", as in 
<pre>
LoggerUtils.logException(logger, new Exception("where is this called from");
</pre>
 * @author jflamy
 */
public class LoggerUtils {

    /**
     * @param logger the logger to use
     * @param t the exception to report
     */
    public static void logException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.info(sw.toString());
    }
    
    /**
     * @param logger the logger to use
     * @param t the exception to report
     */
    public static void logErrorException(Logger logger, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        logger.error(sw.toString());
    }
}
