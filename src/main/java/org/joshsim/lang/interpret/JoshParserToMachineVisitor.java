/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.ParentlessEntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.func.CompiledSelectorFromCallable;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.NoopConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.ChaniningConditionalBuilder;
import org.joshsim.lang.interpret.action.ConditionalAction;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.CompiledCallableFragment;
import org.joshsim.lang.interpret.fragment.ConversionFragment;
import org.joshsim.lang.interpret.fragment.ConversionsFragment;
import org.joshsim.lang.interpret.fragment.EntityFragment;
import org.joshsim.lang.interpret.fragment.EventHandlerGroupFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.fragment.ProgramBuilder;
import org.joshsim.lang.interpret.fragment.ProgramFragment;
import org.joshsim.lang.interpret.fragment.StateFragment;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;


/**
 * Visitor which parses Josh soruce by using Fragments.
 */
@SuppressWarnings("checkstyle:MissingJavaDocMethod")  // Can't use override because of generics.
public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<Fragment> {

  
}
