/**
 * Copyright 2014-2016 the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mewna.plugin.plugins.dnd.dice.parser.listener;

import com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarListener;
import com.mewna.plugin.plugins.dnd.dice.notation.DiceNotationExpression;

/**
 * Visitor for an ANTLR4 parser tree. It can return the fully parsed
 * {@link DiceNotationExpression}.
 * <p>
 * This {@code DiceNotationExpression} is the root for a tree representing the
 * expression received by the parser.
 * 
 * @author Bernardo Martínez Garrido
 */
public interface DiceExpressionBuilder extends DiceNotationGrammarListener {

    /**
     * Returns the root for the tree of dice notation model objects.
     * 
     * @return the tree of dice notation model objects
     */
    public DiceNotationExpression getDiceExpressionRoot();

}
