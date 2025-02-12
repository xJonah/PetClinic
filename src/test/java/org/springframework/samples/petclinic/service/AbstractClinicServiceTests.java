/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.util.EntityUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p> Base class for {@link ClinicService} integration tests. </p> <p> Subclasses should specify Spring context
 * configuration using {@link ContextConfiguration @ContextConfiguration} annotation </p> <p>
 * AbstractclinicServiceTests and its subclasses benefit from the following services provided by the Spring
 * TestContext Framework: </p> <ul> <li><strong>Spring IoC container caching</strong> which spares us unnecessary set up
 * time between test execution.</li> <li><strong>Dependency Injection</strong> of test fixture instances, meaning that
 * we don't need to perform application context lookups. See the use of {@link Autowired @Autowired} on the <code>{@link
 * AbstractClinicServiceTests#clinicService clinicService}</code> instance variable, which uses autowiring <em>by
 * type</em>. <li><strong>Transaction management</strong>, meaning each test method is executed in its own transaction,
 * which is automatically rolled back by default. Thus, even if tests insert or otherwise change database state, there
 * is no need for a teardown or cleanup script. <li> An {@link org.springframework.context.ApplicationContext
 * ApplicationContext} is also inherited and can be used for explicit bean lookup if necessary. </li> </ul>
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
abstract class AbstractClinicServiceTests {

    @Autowired
    protected ClinicService clinicService;

    @Test
    void shouldFindOwnersByLastName() {
        Collection<Owner> owners = this.clinicService.findOwnerByLastName("Davis");
        assertThat(owners).hasSize(2);

        owners = this.clinicService.findOwnerByLastName("Daviss");
        assertThat(owners).isEmpty();
    }

    @Test
    void shouldFindSingleOwnerWithPet() {
        Owner owner = this.clinicService.findOwnerById(1);
        assertThat(owner.getLastName()).startsWith("Franklin");
        assertThat(owner.getPets()).hasSize(1);
        assertThat(owner.getPets().get(0).getType()).isNotNull();
        assertThat(owner.getPets().get(0).getType().getName()).isEqualTo("cat");
    }

    @Test
    @Transactional
    public void shouldInsertOwner() {
        Collection<Owner> owners = this.clinicService.findOwnerByLastName("Schultz");
        int found = owners.size();

        Owner owner = new Owner();
        owner.setFirstName("Sam");
        owner.setLastName("Schultz");
        owner.setAddress("4, Evans Street");
        owner.setCity("Wollongong");
        owner.setTelephone("4444444444");
        this.clinicService.saveOwner(owner);
        assertThat(owner.getId().longValue()).isNotZero();

        owners = this.clinicService.findOwnerByLastName("Schultz");
        assertThat(owners).hasSize(found + 1);
    }

    @Test
    @Transactional
    void shouldUpdateOwner() {
        Owner owner = this.clinicService.findOwnerById(1);
        String oldLastName = owner.getLastName();
        String newLastName = oldLastName + "X";

        owner.setLastName(newLastName);
        this.clinicService.saveOwner(owner);

        // retrieving new name from database
        owner = this.clinicService.findOwnerById(1);
        assertThat(owner.getLastName()).isEqualTo(newLastName);
    }

    @Test
    void shouldFindPetWithCorrectId() {
        Pet pet7 = this.clinicService.findPetById(7);
        assertThat(pet7.getName()).startsWith("Samantha");
        assertThat(pet7.getOwner().getFirstName()).isEqualTo("Jean");

    }

    @Test
    void shouldFindAllPetTypes() {
        Collection<PetType> petTypes = this.clinicService.findPetTypes();

        PetType petType1 = EntityUtils.getById(petTypes, PetType.class, 1);
        assertThat(petType1.getName()).isEqualTo("cat");
        PetType petType4 = EntityUtils.getById(petTypes, PetType.class, 4);
        assertThat(petType4.getName()).isEqualTo("snake");
    }

    @Test
    @Transactional
    public void shouldInsertPetIntoDatabaseAndGenerateId() {
        Owner owner6 = this.clinicService.findOwnerById(6);
        int found = owner6.getPets().size();

        Pet pet = new Pet();
        pet.setName("bowser");
        Collection<PetType> types = this.clinicService.findPetTypes();
        pet.setType(EntityUtils.getById(types, PetType.class, 2));
        pet.setBirthDate(LocalDate.now());
        owner6.addPet(pet);
        assertThat(owner6.getPets()).hasSize(found + 1);

        this.clinicService.savePet(pet);
        this.clinicService.saveOwner(owner6);

        owner6 = this.clinicService.findOwnerById(6);
        assertThat(owner6.getPets()).hasSize(found + 1);
        // checks that id has been generated
        assertThat(pet.getId()).isNotNull();
    }

    @Test
    @Transactional
    public void shouldUpdatePetName() throws Exception {
        Pet pet7 = this.clinicService.findPetById(7);
        String oldName = pet7.getName();

        String newName = oldName + "X";
        pet7.setName(newName);
        this.clinicService.savePet(pet7);

        pet7 = this.clinicService.findPetById(7);
        assertThat(pet7.getName()).isEqualTo(newName);
    }

    @Test
    void shouldFindVets() {
        Collection<Vet> vets = this.clinicService.findVets();

        Vet vet = EntityUtils.getById(vets, Vet.class, 3);
        assertThat(vet.getLastName()).isEqualTo("Douglas");
        assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
        assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
        assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("surgery");
    }

    @Test
    @Transactional
    public void shouldAddNewVisitForPet() {
        Pet pet7 = this.clinicService.findPetById(7);
        int found = pet7.getVisits().size();
        Visit visit = new Visit();
        pet7.addVisit(visit);
        visit.setDescription("test");
        this.clinicService.saveVisit(visit);
        this.clinicService.savePet(pet7);

        pet7 = this.clinicService.findPetById(7);
        assertThat(pet7.getVisits()).hasSize(found + 1);
        assertThat(visit.getId()).isNotNull();
    }

    @Test
    void shouldFindVisitsByPetId() throws Exception {
        Collection<Visit> visits = this.clinicService.findVisitsByPetId(7);
        assertThat(visits).hasSize(2);
        Visit[] visitArr = visits.toArray(new Visit[visits.size()]);
        assertThat(visitArr[0].getPet()).isNotNull();
        assertThat(visitArr[0].getDate()).isNotNull();
        assertThat(visitArr[0].getPet().getId()).isEqualTo(7);
    }

    // Jonah's Tests

	@Test
	void shouldSetValidFirstName() throws Exception {
		Owner owner1 = new Owner();
		Owner owner2 = new Owner();
		Owner owner3 = new Owner();
		String name1 = "Jonah";
		String name2 = "John";
		String name3 = "Joe";
		owner1.setFirstName(name1);
		owner2.setFirstName(name2);
		owner3.setFirstName(name3);
		String owner1Name = owner1.getFirstName();
		String owner2Name = owner2.getFirstName();
		String owner3Name = owner3.getFirstName();
		assertThat(owner1Name.equals(name1)).isTrue();
		assertThat(owner2Name.equals(name2)).isTrue();
		assertThat(owner3Name.equals(name3)).isTrue();
	}

	@Test
	void shouldNotSetInvalidFirstName() {
		Owner owner1 = new Owner();
		Owner owner2 = new Owner();
		Owner owner3 = new Owner();
		String name1 = "wdi2";
		String name2 = "wad<>";
		String name3 = "jonah^^@''";
		owner1.setFirstName(name1);
		owner2.setFirstName(name2);
		owner3.setFirstName(name3);
		String owner1Name = owner1.getFirstName();
		String owner2Name = owner2.getFirstName();
		String owner3Name = owner3.getFirstName();
		assertThat(owner1Name.equals(name1)).isFalse();
		assertThat(owner2Name.equals(name2)).isFalse();
		assertThat(owner3Name.equals(name3)).isFalse();
	}

	@Test
	void shouldSetValidPhoneNumber() throws Exception {
		Owner owner1 = new Owner();
		Owner owner2 = new Owner();
		Owner owner3 = new Owner();
		String phone1 = "01580123123";
		String phone2 = "116123";
		String phone3 = "07824123123";
		owner1.setTelephone(phone1);
		owner2.setTelephone(phone2);
		owner3.setTelephone(phone3);
		String owner1Phone = owner1.getTelephone();
		String owner2Phone = owner2.getTelephone();
		String owner3Phone = owner3.getTelephone();
		assertThat(owner1Phone.equals(phone1)).isTrue();
		assertThat(owner2Phone.equals(phone2)).isTrue();
		assertThat(owner3Phone.equals(phone3)).isTrue();
	}

	@Test
	void shouldNotSetInvalidPhoneNumber() {
		Owner owner1 = new Owner();
		Owner owner2 = new Owner();
		Owner owner3 = new Owner();
		String phone1 = "abc123";
		String phone2 = "999";
		String phone3 = "111";
		owner1.setTelephone(phone1);
		owner2.setTelephone(phone2);
		owner3.setTelephone(phone3);
		String owner1Phone = owner1.getTelephone();
		String owner2Phone = owner2.getTelephone();
		String owner3Phone = owner3.getTelephone();
		assertThat(owner1Phone.equals(phone1)).isFalse();
		assertThat(owner2Phone.equals(phone2)).isFalse();
		assertThat(owner3Phone.equals(phone3)).isFalse();
	}

	@Test
	void shouldNotSetInvalidBirthDate() {
		Pet pet1 = new Pet();
		Pet pet2 = new Pet();
		Pet pet3 = new Pet();
		LocalDate date1 = LocalDate.of(1800, 1, 1);
		LocalDate date2 = LocalDate.of(3000, 1, 1);
		LocalDate date3 = LocalDate.of(2025, 12, 1);
		pet1.setBirthDate(date1);
		pet2.setBirthDate(date2);
		pet3.setBirthDate(date3);
		LocalDate pet1Date = pet1.getBirthDate();
		LocalDate pet2Date = pet2.getBirthDate();
		LocalDate pet3Date = pet3.getBirthDate();
		assertThat(pet1Date.equals(date1)).isFalse();
		assertThat(pet2Date.equals(date2)).isFalse();
		assertThat(pet3Date.equals(date3)).isFalse();
	}


}
