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

package com.mewna.plugin.plugins.dnd.dice.roller;

import com.mewna.plugin.plugins.dnd.dice.Dice;
import com.mewna.plugin.plugins.dnd.dice.roller.random.NumberGenerator;
import com.mewna.plugin.plugins.dnd.dice.roller.random.RandomNumberGenerator;

import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates random integer values from a {@link Dice}.
 * <p>
 * To generate the actual random values a {@code NumberGenerator} is used.
 * 
 * @author Bernardo Martínez Garrido
 * @see NumberGenerator
 * @see Dice
 */
public final class DefaultRoller implements com.mewna.plugin.plugins.dnd.dice.roller.Roller {

    /**
     * The random numbers generator.
     * <p>
     * Combined with the data in the rolled this, this will generate a random
     * value in an interval.
     */
    private final NumberGenerator numGen;

    /**
     * Default constructor.
     */
    public DefaultRoller() {
        super();

        this.numGen = new RandomNumberGenerator();
    }

    /**
     * Constructs a roller with the specified random number generator.
     * 
     * @param generator
     *            the random number generator to use
     */
    public DefaultRoller(final NumberGenerator generator) {
        super();

        numGen = checkNotNull(generator,
                "Received a null pointer as generator");
    }

    /**
     * Generates a collection of random values from the received {@code Dice}.
     * <p>
     * These are returned in the same order they were com.mewna.plugin.plugins.dnd.dice.generated.
     * 
     * @param dice
     *            the dice to roll
     * @return a collection of random values com.mewna.plugin.plugins.dnd.dice.generated from the dice
     */
    @Override
    public final Iterable<Integer> roll(final Dice dice) {
        final Collection<Integer> rolls; // Roll results

        checkNotNull(dice, "Received a null pointer as dice");
        checkArgument(dice.getQuantity() >= 0,
                "The quantity of dice can not be negative");
        checkArgument(dice.getSides() >= 0,
                "The number of sides can not be negative");
        checkArgument(dice.getQuantity() <= 100, "Cannot roll more than 100 dice!");
        checkArgument(dice.getSides() <= 10000, "Cannot roll dice with more than 10000 sides!");

        rolls = new LinkedList<Integer>();
        for (Integer i = 0; i < dice.getQuantity(); i++) {
            rolls.add(getNumberGenerator().generate(dice.getSides()));
        }

        return rolls;
    }

    /**
     * Returns the random number generator.
     * 
     * @return the random number generator
     */
    private final NumberGenerator getNumberGenerator() {
        return numGen;
    }

}
