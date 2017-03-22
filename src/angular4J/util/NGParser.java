package angular4J.util;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import angular4J.events.NGEvent;
import angular4J.io.ByteArrayCache;

/**
 * utility class for Angular4J
 */
@SuppressWarnings("serial")
public class NGParser implements Serializable {

   private NGParser() {
      this.initJsonSerialiser();
   }

   private static NGParser instance;

   private transient Gson mainSerializer;
   private HttpServletRequest request;

   public static JsonElement parseMessage(String message) {
      if (message == null) {
         return null;
      }
      if (!message.startsWith("{")) {
         return new JsonPrimitive(message);
      }
      JsonParser parser = new JsonParser();

      return parser.parse(message);
   }

   private static final void createInstance() {
      instance = new NGParser();
   }

   public static final synchronized NGParser getInstance() {
      if (instance == null) {
         createInstance();
      }
      return instance;
   }

   private class ByteArrayJsonAdapter implements JsonSerializer<Object> {

      public ByteArrayJsonAdapter() {}

      private String getContextPath(HttpServletRequest request) {
         StringBuilder contextPath = new StringBuilder(request.getScheme());
         contextPath.append("://");
         contextPath.append(request.getServerName());
         contextPath.append(":");
         contextPath.append(request.getServerPort());
         contextPath.append(request.getServletContext().getContextPath());
         contextPath.append("/");

         return contextPath.toString();
      }

      @Override
      public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
         if (request == null) {
            return null;
         }

         byte[] bytes = null;
         if (src instanceof NGLob) {
            bytes = ((NGLob) src).getBytes();
         } else if (src instanceof byte[]) {
            bytes = (byte[]) src;
         } else {
            return null;
         }

         String id = String.valueOf(UUID.randomUUID());
         ByteArrayCache.getInstance().getCache().put(id, bytes);

         String result = this.getContextPath(request) + "lob/" + id + "?" + Calendar.getInstance().getTimeInMillis();

         return new JsonPrimitive(result);
      }
   }

   private byte[] getBytesFromJson(JsonElement element) {
      String value = element.getAsString();
      if (CommonUtils.isStrValid(value)) {
         try {
            if (value.contains(Constants.DATA_MARK) && value.contains(Constants.BASE64_MARK)) {
               value = value.substring(value.indexOf(Constants.BASE64_MARK) + Constants.BASE64_MARK.length());
            }
            if (value.length() > 0) {
               return DatatypeConverter.parseBase64Binary(value);
            }
         }
         catch (Exception e) {}
      }
      return null;
   }

   private JsonPrimitive getBase64Json(String type, byte[] bytes) {
      if (bytes != null && bytes.length > 0) {
         if (type == null || type.trim().length() == 0) {
            type = NGBase64.FORM_DATA_TYPE;
         }

         try {
            return new JsonPrimitive(Constants.DATA_MARK + type + Constants.BASE64_MARK + Base64.getEncoder().encodeToString(bytes).trim());
         }
         catch (Exception e) {}

      }
      return null;
   }

   public String serialize(Object object) {
      return this.serialize(object, null);
   }

   public String serialize(Object object, HttpServletRequest request) {
      if (object == null) {
         return null;
      }

      this.request = request;

      return mainSerializer.toJson(object);
   }

   public Object deserialize(JsonElement element, Type type) {
      return mainSerializer.fromJson(element, type);
   }

   public Object deserialize(String json, Type type) {
      return mainSerializer.fromJson(json, type);
   }

   public Object convertEvent(NGEvent event) throws ClassNotFoundException {

      JsonElement element = parseMessage(event.getData());

      JsonElement data;
      Class<?> javaClass;

      try {
         data = element.getAsJsonObject();

         javaClass = Class.forName(event.getDataClass());
      }
      catch (Exception e) {
         data = element.getAsJsonPrimitive();
         if (event.getDataClass() == null) {
            event.setDataClass("String");
         }
         javaClass = Class.forName("java.lang." + event.getDataClass());

      }

      Object o;
      if (javaClass.equals(String.class)) {
         o = data.toString().substring(1, data.toString().length() - 1);
      } else {
         o = deserialize(data, javaClass);
      }
      return o;
   }

   public Object deserialiseFromString(String value, Class<?> type) {
      if (value == null) {
         return null;
      }

      if (String.class.equals(type)) {
         return value;
      }

      if (type.equals(int.class) || type.equals(Integer.class)) {
         return Integer.parseInt(value);
      }
      if (type.equals(float.class) || type.equals(Float.class)) {
         return Float.parseFloat(value);
      }
      if (type.equals(boolean.class) || type.equals(Boolean.class)) {
         return Boolean.parseBoolean(value);
      }
      if (type.equals(double.class) || type.equals(Double.class)) {
         return Double.parseDouble(value);
      }
      if (type.equals(byte.class) || type.equals(Byte.class)) {
         return Byte.parseByte(value);
      }
      if (type.equals(long.class) || type.equals(Long.class)) {
         return Long.parseLong(value);
      }
      if (type.equals(short.class) || type.equals(Short.class)) {
         return Short.parseShort(value);
      }

      throw new IllegalArgumentException("unknown primitive type :" + type.getCanonicalName());
   }

   public void initJsonSerialiser() {
      GsonBuilder builder = new GsonBuilder();

      builder.serializeNulls();

      builder.setExclusionStrategies(NGConfig.getGsonExclusionStrategy());

      builder.registerTypeAdapter(NGLob.class, new ByteArrayJsonAdapter());

      builder.registerTypeAdapter(NGBase64.class, new JsonSerializer<NGBase64>(){

         @Override
         public JsonElement serialize(NGBase64 src, Type typeOfSrc, JsonSerializationContext context) {
            if (src != null) {
               return getBase64Json(src.getType(), src.getBytes());
            }
            return null;
         }
      });

      builder.registerTypeAdapter(NGBase64.class, new JsonDeserializer<NGBase64>(){

         @Override
         public NGBase64 deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {
            byte[] bytes = getBytesFromJson(element);
            if (bytes != null && bytes.length > 0) {
               return new NGBase64(bytes);
            }
            return null;
         }
      });

      builder.registerTypeAdapter(byte[].class, new JsonSerializer<byte[]>(){

         @Override
         public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return getBase64Json(null, src);
         }
      });

      builder.registerTypeAdapter(byte[].class, new JsonDeserializer<byte[]>(){

         @Override
         public byte[] deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {
            return getBytesFromJson(element);
         }
      });

      final SimpleDateFormat dateFormat = new SimpleDateFormat("'" + Constants.DATA_MARK + Constants.DATE_UTC_MARK + "'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

      if (dateFormat != null && NGConfig.getProperty("TIME_ZONE") != null) {
         dateFormat.setTimeZone(TimeZone.getTimeZone(NGConfig.getProperty("TIME_ZONE")));
      }

      builder.registerTypeAdapter(java.sql.Date.class, new JsonSerializer<java.sql.Date>(){

         @Override
         public JsonElement serialize(java.sql.Date src, Type typeOfSrc, JsonSerializationContext context) {

            if (src != null) {
               Calendar cal = Calendar.getInstance();
               cal.setTime(src);
               cal.set(Calendar.HOUR_OF_DAY, 0);
               cal.set(Calendar.MINUTE, 0);
               cal.set(Calendar.SECOND, 0);
               cal.set(Calendar.MILLISECOND, 0);

               return new JsonPrimitive(dateFormat.format(cal.getTime()));
            }
            return null;
         }
      });

      builder.registerTypeAdapter(java.sql.Date.class, new JsonDeserializer<java.sql.Date>(){

         @Override
         public java.sql.Date deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {

            try {
               String dateFormated = element.getAsString();
               if (CommonUtils.isStrValid(dateFormated)) {
                  Calendar cal = Calendar.getInstance();
                  cal.setTime(dateFormat.parse(dateFormated));
                  cal.set(Calendar.HOUR_OF_DAY, 0);
                  cal.set(Calendar.MINUTE, 0);
                  cal.set(Calendar.SECOND, 0);
                  cal.set(Calendar.MILLISECOND, 0);
                  return new java.sql.Date(cal.getTime().getTime());
               }
            }
            catch (Exception e) {}

            return null;
         }

      });

      builder.registerTypeAdapter(java.sql.Time.class, new JsonSerializer<java.sql.Time>(){

         @Override
         public JsonElement serialize(java.sql.Time src, Type typeOfSrc, JsonSerializationContext context) {
            if (src != null) {
               Calendar cal = Calendar.getInstance();
               cal.setTime(src);

               return new JsonPrimitive(dateFormat.format(cal.getTime()));
            }
            return null;
         }

      });

      builder.registerTypeAdapter(java.sql.Time.class, new JsonDeserializer<java.sql.Time>(){

         @Override
         public java.sql.Time deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {

            try {
               String dateFormated = element.getAsString();
               if (CommonUtils.isStrValid(dateFormated)) {
                  Calendar cal = Calendar.getInstance();
                  cal.setTime(dateFormat.parse(dateFormated));

                  return new java.sql.Time(cal.getTime().getTime());
               }
            }
            catch (Exception e) {}

            return null;
         }

      });

      builder.registerTypeAdapter(java.sql.Timestamp.class, new JsonSerializer<java.sql.Timestamp>(){

         @Override
         public JsonElement serialize(java.sql.Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
            if (src != null) {
               Calendar cal = Calendar.getInstance();
               cal.setTime(src);

               return new JsonPrimitive(dateFormat.format(cal.getTime()));
            }
            return null;
         }

      });

      builder.registerTypeAdapter(java.sql.Timestamp.class, new JsonDeserializer<java.sql.Timestamp>(){

         @Override
         public java.sql.Timestamp deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {

            try {
               String dateFormated = element.getAsString();
               if (CommonUtils.isStrValid(dateFormated)) {
                  Calendar cal = Calendar.getInstance();
                  cal.setTime(dateFormat.parse(dateFormated));

                  return new java.sql.Timestamp(cal.getTime().getTime());
               }
            }
            catch (Exception e) {}

            return null;
         }

      });

      builder.registerTypeAdapter(java.util.Date.class, new JsonSerializer<java.util.Date>(){

         @Override
         public JsonElement serialize(java.util.Date src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? null : new JsonPrimitive(dateFormat.format(src));
         }

      });

      builder.registerTypeAdapter(java.util.Date.class, new JsonDeserializer<java.util.Date>(){

         @Override
         public java.util.Date deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {

            try {
               String dateFormated = element.getAsString();
               if (CommonUtils.isStrValid(dateFormated)) {
                  return dateFormat.parse(dateFormated);
               }
            }
            catch (Exception e) {}

            return null;
         }

      });

      mainSerializer = builder.create();
   }
}