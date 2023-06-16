package org.acme;

import ga.josejulio.versioned.path.VersionedMethod;
import ga.josejulio.versioned.path.VersionedPath;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@VersionedPath(path = "/api/v$version/pets", sinceVersion = "1.0")
public class PetResource {

    static final Map<String, Pet> pets = new HashMap<>();

    @VersionedPath(path = "/", sinceVersion = "1.0")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    public void addPet(@QueryParam("name") String name) {
        Pet pet = new Pet(name);
        pets.put(name, pet);
    }

    @VersionedPath(path = "/", sinceVersion = "2.0")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    public void addPetThrowsIfFound(@QueryParam("name") String name) {
        if (pets.containsKey(name)) {
            throw new BadRequestException("Pet with name: %s already exists.".formatted(name));
        }

        addPet(name);
    }

    @VersionedPath(path = "/", sinceVersion = "3.1")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    public Pet addPetAndReturnIt(@QueryParam("name") String name) {
        addPetThrowsIfFound(name);
        return pets.get(name);
    }

    // We don't need to annotate it differently if we are using it on all the versions
    @DELETE
    public void removePet(@QueryParam("name") String name) {
        pets.remove(name);
    }

    @GET
    public Collection<Pet> getPets() {
        return pets.values();
    }
}
