package nl.tudelft.contextproject.tygron.eis;

import eis.exceptions.EntityException;
import eis.exceptions.ManagementException;
import eis.iilang.Action;
import eis.iilang.EnvironmentState;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import nl.tudelft.contextproject.tygron.api.Connector;
import nl.tudelft.contextproject.tygron.api.Session;
import nl.tudelft.contextproject.tygron.eis.entities.Controller;
import nl.tudelft.contextproject.tygron.eis.translators.ConfigurationTranslator;
import nl.tudelft.contextproject.tygron.eis.translators.HashMapTranslator;
import nl.tudelft.contextproject.tygron.eis.translators.ParamEnumTranslator;
import eis.eis2java.environment.AbstractEnvironment;
import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;

import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("serial")
public class TygronInterfaceImpl extends AbstractEnvironment {

  protected Connector connector;
  protected Session controller;
  protected Configuration configuration;
  
  /**
   * Constructs the EIS.
   */
  public TygronInterfaceImpl() {
    Translator translator = Translator.getInstance();
    translator.registerParameter2JavaTranslator(new ConfigurationTranslator());
    translator.registerParameter2JavaTranslator(new HashMapTranslator());
    translator.registerParameter2JavaTranslator(new ParamEnumTranslator());
  }
  
  /* (non-Javadoc)
   * @see eis.EIDefaultImpl#isSupportedByEnvironment(eis.iilang.Action)
   */
  @Override
  protected boolean isSupportedByEnvironment(Action action) {
    return false;
  }

  /* (non-Javadoc)
   * @see eis.EIDefaultImpl#isSupportedByType(eis.iilang.Action, java.lang.String)
   */
  @Override
  protected boolean isSupportedByType(Action action, String type) {
    return false;
  }

  /* (non-Javadoc)
   * @see eis.EIDefaultImpl#init(java.util.Map)
   */
  @Override
  public void init(Map<String, Parameter> parameters)
      throws ManagementException {
    
    try {
      // Wrapper pending fix to environment init.
      ParameterList parameterMap = new ParameterList();
      for (Entry<String,Parameter> entry : parameters.entrySet()) {
        parameterMap.add(new ParameterList(new Identifier(entry.getKey()), entry.getValue()));
      }
      configuration = Translator.getInstance().translate2Java(
          parameterMap, Configuration.class);
    } catch (TranslationException e) {
      throw new ManagementException("Invalid parameters", e);
    }
    
    // Prepare the game.
    reset(parameters);
    
    // Try creating and registering an entity.
    try {
      registerEntity("Controller", "MainEntity", 
          new Controller(controller.getEnvironment()));
    } catch (EntityException e) {
      throw new ManagementException("Could not create an entity", e);
    }
  }

  /* (non-Javadoc)
   * @see eis.eis2java.environment.AbstractEnvironment#reset(java.util.Map)
   */
  @Override
  public void reset(Map<String, Parameter> parameters)
      throws ManagementException {
    
    if (connector != null) {
      connector.cleanup();
    }
    
    //Create a new connection
    connector = new Connector();
    
    //Get the session manager
    controller = connector.getSession();
    
    setState(EnvironmentState.PAUSED);
  }

  /* (non-Javadoc)
   * @see eis.EIDefaultImpl#kill()
   */
  @Override
  public void kill() throws ManagementException {
    
    if (connector != null) {
      connector.cleanup();
    }
    
    setState(EnvironmentState.KILLED);
  }

}
