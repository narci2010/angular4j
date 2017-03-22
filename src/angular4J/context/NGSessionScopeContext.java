package angular4J.context;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import angular4J.util.Constants;
import angular4J.util.Pair;

/**
 * 
 * A custom CDI context implementation bound to the NGSession (cross context between the Websockets
 * session and HTTP Session)
 */
@SuppressWarnings("serial")
public class NGSessionScopeContext implements Context, Serializable {

   private static NGSessionScopeContext instance;

   private ThreadLocal<Pair<String, NGSessionContextHolder>> holder = new ThreadLocal<>();

   private NGSessionScopeContext() {}

   private static final void createInstance() {
      instance = new NGSessionScopeContext();
   }

   public static final synchronized NGSessionScopeContext getInstance() {
      if (instance == null) {
         createInstance();
      }
      return instance;
   }

   public void setCurrentContext(String sessionID) {
      this.holder.set(new Pair(sessionID, GlobalNGSessionContextsHolder.getInstance().getSession(sessionID)));
   }

   public String getCurrentSessionID() {
      return this.holder.get().getKey();
   }

   @Override
   public Class<? extends Annotation> getScope() {
      return NGSessionScoped.class;
   }

   @Override
   public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
      if (this.holder.get() == null) {
         return null;
      }

      Bean bean = (Bean) contextual;
      if (this.holder.get().getValue().getBeans().containsKey(bean.getBeanClass())) {
         return (T) this.holder.get().getValue().getBean(bean.getBeanClass()).getInstance();
      } else {

         T instance = (T) bean.create(creationalContext);

         NGSessionScope customScope = new NGSessionScope();
         customScope.setBean(bean);
         customScope.setCtx(creationalContext);
         customScope.setInstance(instance);

         this.holder.get().getValue().putBean(customScope);

         return instance;
      }
   }

   @Override
   public <T> T get(Contextual<T> contextual) {
      Bean bean = (Bean) contextual;

      if (this.holder.get().getValue().getBeans().containsKey(bean.getBeanClass())) {
         return (T) this.holder.get().getValue().getBean(bean.getBeanClass()).getInstance();
      } else {
         return null;
      }
   }

   @Override
   public boolean isActive() {
      return true;
   }

   public final boolean isScopeSession() {
      return !this.getCurrentSessionID().equals(Constants.GENERATE_SESSION_ID);
   }

   public final boolean isGenerateSession() {
      return this.getCurrentSessionID().equals(Constants.GENERATE_SESSION_ID);
   }
}
