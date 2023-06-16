package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class PetResourceTest {

    @ParameterizedTest
    @MethodSource("provideVersions")
    public void listPetsWorksOnAllVersionsTest(String version) {
        try {
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/%s/pets".formatted(version))
                    .then()
                    .statusCode(anyOf(
                            is(200),
                            is(204)
                    ));

            Pet[] pets = given()
                    .when()
                    .get("/api/%s/pets".formatted(version))
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .as(Pet[].class);

            assertEquals(1, pets.length);
            assertEquals("Foo", pets[0].name());
        } finally {
            PetResource.pets.clear();
        }
    }

    @Test
    public void addV1DoesNotReturn() {
        try {
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v1.0/pets")
                    .then()
                    .statusCode(204);
        } finally {
            PetResource.pets.clear();
        }
    }

    @Test
    public void addV2DoesNotReturn() {
        try {
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v2.0/pets")
                    .then()
                    .statusCode(204);
        } finally {
            PetResource.pets.clear();
        }
    }

    @Test
    public void addV3ReturnPet() {
        try {
            Pet pet = given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v3.1/pets")
                    .then()
                    .statusCode(200)
                    .extract().as(Pet.class);

            assertEquals("Foo", pet.name());
        } finally {
            PetResource.pets.clear();
        }
    }

    @Test
    public void addV1OverridesWhenRepeatingNameTest() {
        try {
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v1.0/pets")
                    .then()
                    .statusCode(204);
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v1.0/pets")
                    .then()
                    .statusCode(204);

            assertEquals(1, PetResource.pets.size());
        } finally {
            PetResource.pets.clear();
        }
    }

    @Test
    public void addV2FailsWhenRepeatingNameTest() {
        try {
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v2.0/pets")
                    .then()
                    .statusCode(204);
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v2.0/pets")
                    .then()
                    .statusCode(400);

            assertEquals(1, PetResource.pets.size());
        } finally {
            PetResource.pets.clear();
        }
    }

    @Test
    public void addV3FailsWhenRepeatTest() {
        try {
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v3.1/pets")
                    .then()
                    .statusCode(200);
            given()
                    .when()
                    .queryParam("name", "Foo")
                    .post("/api/v3.1/pets")
                    .then()
                    .statusCode(400);

            assertEquals(1, PetResource.pets.size());
        } finally {
            PetResource.pets.clear();
        }
    }

    private static Stream<String> provideVersions() {
        return Stream.of(
                "v1.0",
                "v2.0",
                "v3.1"
        );
    }

}
