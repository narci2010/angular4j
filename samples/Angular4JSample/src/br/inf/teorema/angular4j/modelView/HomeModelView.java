package br.inf.teorema.angular4j.modelView;

import javax.annotation.PostConstruct;

import angular4J.api.Angular4J;
import angular4J.api.NGCastMap;
import angular4J.api.http.Get;
import angular4J.api.http.Post;
import angular4J.context.NGSessionScoped;
import angular4J.util.NGLob;
import angular4J.util.NGType;
import angular4J.util.NGTypeMap;
import br.inf.teorema.angular4j.model.Info;
import br.inf.teorema.angular4j.singleton.SystemControl;

@SuppressWarnings("serial")
@Angular4J
@NGSessionScoped
public class HomeModelView extends GenericModelView {

   @NGCastMap
   protected static NGTypeMap ngCastMap = new NGTypeMap();
   {
      ngCastMap.add("ngModel", new NGType<Info>(){});
   }

   private String imageAngular4j;

   @PostConstruct
   public void postConstruct() {
      this.imageAngular4j = "assets/img/logotipo_angular4J-01.png";
   }

   @Get
   public String getImageAngular4j() {
      return this.imageAngular4j;
   }

   @Get
   public Info getInfo() {
      return SystemControl.getInstance().getInfo(this.getSession().getId());
   }

   @Post
   public Info restore() {
      SystemControl.getInstance().restore(this.getSession().getId());
      return this.getInfo();
   }

   @Override
   public Info reload() {
      return this.getInfo();
   }

   @Get
   public NGLob downloadImage() {
      return new NGLob(this.getInfo().getImage());
   }
}