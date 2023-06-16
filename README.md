# Versioned path
[![Release](https://jitpack.io/v/josejulio/versioned-path.svg)](https://jitpack.io/#josejulio/versioned-path)

A library to reuse rest endpoints in multiple API versions within the same application

## About

The goal of `versioned-path` is to create a library that could help reuse code in multiple API versions within the same
application.

Imagine your API is on continues development under version `1.x`. But at some point in time you need to introduce a 
breaking change (different return type or inputs). 

You could create a new endpoint, but that might look strange.

- /api/v1.0/pets/addPet
- /api/v1.0/pets/addPetWithNewReturn

or maybe a new branch / repository with the v2.0 API. but then you would have to host 2 applications which might be fine.

This library helps to leverage code that uses rest annotations (`@Path`, `@GET`, etc) to serve multiple versions of the API within the same codebase.
You can decide which endpoints to override while keeping all the others endpoints.

## Usage

Annotate the class with `@VersionedPath` indicating the path and using `$version` as the placeholder for the version,
do the same for every method that is different on each version.

```java
@VersionedPath(path = "/api/v$version/pets", sinceVersion = "1.0")
public class PetResource {
    @VersionedPath(path = "/", sinceVersion = "1.0")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    public void addPet(@QueryParam("name") String name);

    @VersionedPath(path = "/", sinceVersion = "2.0")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    public void addPetThrowsIfFound(@QueryParam("name") String name);

    @VersionedPath(path = "/", sinceVersion = "3.1")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    public Pet addPetAndReturnIt(@QueryParam("name") String name);

    // We don't need to annotate it differently if we are using it on all the versions
    @jakarta.ws.rs.DELETE
    public void removePet(@QueryParam("name") String name);
}
```

This will generate the following paths:
 
 - v1.0
   - POST   api/v1.0/pets
   - DELETE api/v1.0/pets
 - v2.0
    - POST   api/v2.0/pets
    - DELETE api/v2.0/pets
 - v3.1
    - POST   api/v3.1/pets
    - DELETE api/v3.1/pets


An example is included in this repository, you can see it [here](./versioned-api-sample).

## Installing

It's not pushed to maven central. Instead it can be downloaded from [jitpack](https://jitpack.io/).

Add the following to your `pom.xml`:

```xml
<project>
    <repositories>
        <repository>
            <id>jitpack</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>com.github.josejulio.versioned-path</groupId>
            <artifactId>versioned-path</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>

   <plugin>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>${compiler-plugin.version}</version>
      <configuration>
         <annotationProcessors>
            <annotationProcessor>ga.josejulio.versioned.path.VersionedAnnotationProcessor</annotationProcessor>
         </annotationProcessors>
      </configuration>
   </plugin>
</project>
```

## How it works

This makes use of the [Annotation Processing API](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html).
All the `@VersionedPath` and `@VersionedMethod` are detected and new classes are created that make use of the
`@jakarta.ws.rs.Path` (and friends) annotations. Above example creates the following classes:

```java
// PetResourceV1_0
@Path("/api/v1.0/pets")
public class PetResource {
   @Path("/")
   @POST
   public void addPet(@QueryParam("name") String name) {
       super.addPet(name);
   }
}

// PetResourceV2_0
@Path("/api/v2.0/pets")
public class PetResource {
   @Path("/")
   @POST
   public void addPetThrowsIfFound(@QueryParam("name") String name) {
      super.addPetThrowsIfFound(name);
   }
}

// PetResourceV3_1
@Path("/api/v3.1/pets")
public class PetResource {
   @Path("/")
   @POST
   public Pet addPetAndReturnIt(@QueryParam("name") String name) {
      super.addPetAndReturnIt(name);
   }
}
```

They all inherit from the base class `PetResource` and get the common methods while only providing access to the
new methods.

## FAQ (not really, I just made it up)

### What if I use javax.ws.rs.Path?
It should be easy to implement - we could add support for parameters to decide which one to use and/or trying to check
which one is available in the classpath (e.g. using `Class.forName()`) and use it. PRs are welcome!

### What about ...?
I was not aware of any other method other than subclassing and I though it was interesting to play with the
[Annotation Processing API](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html).
