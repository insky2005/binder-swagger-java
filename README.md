# binder-swagger-java

[![Build Status](https://travis-ci.org/tminglei/binder-swagger-java.svg?branch=master)](https://travis-ci.org/tminglei/binder-swagger-java)

I noticed, if we provided the `swagger.json`, [`swagger ui`](http://petstore.swagger.io/) will take care of the others: dynamically build the web client, enable us online browsing the APIs, sending request to and receiving response from the real services.

`binder-swagger-java` was designed to help construct the swagger object (corresponding to `swagger.json`), and let it accessible from swagger ui or other http visitors.


## How it works
You define the api meta data in classes' static code blocks, then it was collected to a static global swagger object when class scan/loading, so when requested, the program can serve it right now.

![binder-swagger description](https://raw.githubusercontent.com/tminglei/binder-swagger-java/master/binder-swagger-java.png)

> _p.s. `binder-swagger-java` based on [`form-binder-java`](https://github.com/tminglei/form-binder-java) and [`swagger-models`](https://github.com/swagger-api/swagger-core), allow to define dynamic data structures and operate the swagger object directly when necessary, so it's more expressive in theory._


## How to use
#### 0) add the dependency to your project:
```xml
<dependency>
  <groupId>com.github.tminglei</groupId>
  <artifactId>binder-swagger-java</artifactId>
  <version>0.6.2</version>
</dependency>
```
#### 1) define and register your api operations:
```java
// in `PetResource.java`
static Mapping<?> petStatus = $(text(oneOf(Arrays.asList("available", "pending", "sold"))))
    .desc("pet status in the store").$$;
static Mapping<?> pet = $(mapping(
    field("id", $(vLong()).desc("pet id").$$),
    field("name", $(text(required())).desc("pet name").$$),
    field("category", attach(required()).to($(mapping(
            field("id", vLong(required())),
            field("name", text(required()))
    )).refName("category").desc("category belonged to").$$)),
    field("photoUrls", $(list(text())).desc("pet's photo urls").$$),
    field("tags", $(list(text())).desc("tags for the pet").$$),
    field("status", petStatus)
)).refName("pet").desc("pet info").$$;

static {
    operation("post", "/pet")
        .summary("create a pet")
        .tag("pet")
        .parameter(param(pet).in("body"))
        .response(200, response().description("success"))
        .response(400, response())
    ;
}
@POST
public Response addPet(String data) throws BadRequestException, SQLException {
...
```
#### 2) supplement your other swagger info:
```java
// in `Bootstrap.java`
static {  // for swagger
	swagger().info(info()
	      .title("Swagger Sample App")
	      .description("This is a sample server Petstore server.  You can find out more about Swagger " +
		      "at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, " +
		      "you can use the api key `special-key` to test the authorization filters.")
	      .termsOfService("http://swagger.io/terms/")
	      .contact(contact().email("apiteam@swagger.io"))
	      .license(license().name("Apache 2.0")
		              .url("http://www.apache.org/licenses/LICENSE-2.0.html")
	      )
	).host("localhost:8002")
	.basePath("/api")
	.consumes("application/json")
	.produces("application/json")
	.securityDefinition("api_key", apiKeyAuth("api_key", In.HEADER))
	.securityDefinition("petstore_auth", oAuth2()
		      .implicit("http://petstore.swagger.io/api/oauth/dialog")
		      .scope("read:pets", "read your pets")
		      .scope("write:pets", "modify pets in your account")
	).tag(tag("pet").description("Everything about your Pets")
		      .externalDocs(externalDocs().description("Find out more").url("http://swagger.io"))
	).tag(tag("store").description("Access to Petstore orders")
	).tag(tag("user").description("Operations about user")
	      .externalDocs(externalDocs().description("Find out more about our store").url("http://swagger.io"))
	);
}
```
#### 3) configure the filter, which will serv the `swagger.json`:
```xml
// in `web.xml`
<filter>
    <filter-name>SwaggerFilter</filter-name>
    <filter-class>com.github.tminglei.swagger.SwaggerFilter</filter-class>
    <!-- configure your extended swagger helper
    <init-param>
        <param-name>my-extended-swagger-helper</param-name>
        <param-value>com.mycompany.pkg.MySwaggerHelper</param-value>
    </init-param>
    -->
    <init-param>
        <param-name>scan-packages-and-classes</param-name>
        <param-value>com.example.resource; com.example.Bootstrap</param-value>
    </init-param>
    <!-- specify the path to fetch your swagger json, default '/swagger.json'
    <init-param>
        <param-name>swagger-path</param-name>
        <param-value>/swagger.json</param-value>
    </init-param>
    -->
    <!-- enable/disable swagger, default value: true
    <init-param>
        <param-name>enabled</param-name>
        <param-value>false</param-value>
    </init-param>
    -->
</filter>
<filter-mapping>
    <filter-name>SwaggerFilter</filter-name>
    <url-pattern>/api/*</url-pattern><!-- keep consistent with the jersey servlet mapping -->
</filter-mapping>
...
```
NOTES: if you extend the [MSwaggerHelper](https://github.com/tminglei/binder-swagger-java/blob/master/src/main/java/com/github/tminglei/swagger/MSwaggerHelper.java) and configure it here, pls ensure **SwaggerFilter** to be loaded eariler, that is, ensure the swagger helper to be set to [SwaggerContext](https://github.com/tminglei/binder-swagger-java/blob/master/src/main/java/com/github/tminglei/swagger/SwaggerContext.java) before other class scanner started working.


##### That's all. Enjoy it!


> For more usage details, pls check the example project [here](https://github.com/tminglei/binder-swagger-java/tree/master/example/java-jaxrs).


## Q & A
**Q:** Why use static code blocks to associate/register operation meta info instead of annotations?  
**A:** Well, because we can't use annotations here. Annotation requires static defined data types, but we didn't define java beans in our project.  
_(p.s. because of this, we can't also use existing frameworks, like `springfox`.)_


## License
The BSD License, Minglei Tu &lt;tmlneu@gmail.com&gt;
