/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaemr.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.TbMetadata;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link EmrUtils}
 */
public class EmrUtilsTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private CommonMetadata commonMetadata;

	@Autowired
	private TbMetadata tbMetadata;

	/**
	 * Setup each test
	 */
	@Before
	public void setup() throws Exception {
		executeDataSet("dataset/test-concepts.xml");

		commonMetadata.install();
		tbMetadata.install();
	}

	@Test
	public void integration() {
		new EmrUtils();
	}

	/**
	 * @see EmrUtils#dateHasTime(java.util.Date)
	 * @verifies return true only if date has time
	 */
	@Test
	public void dateHasTime_shouldReturnTrueOnlyInDateHasTime() {
		Assert.assertFalse(EmrUtils.dateHasTime(TestUtils.date(2012, 1, 1)));
		Assert.assertTrue(EmrUtils.dateHasTime(TestUtils.date(2012, 1, 1, 10, 0, 0)));
		Assert.assertTrue(EmrUtils.dateHasTime(TestUtils.date(2012, 1, 1, 0, 10, 0)));
		Assert.assertTrue(EmrUtils.dateHasTime(TestUtils.date(2012, 1, 1, 0, 0, 10)));
	}

	/**
	 * @see EmrUtils#isToday(java.util.Date)
	 * @verifies return true only for dates that are today
	 */
	@Test
	public void isToday_shouldReturnTrueOnlyForDatesThatAreToday() {
		Assert.assertTrue(EmrUtils.isToday(new Date()));
		Assert.assertFalse(EmrUtils.isToday(TestUtils.date(2012, 1, 1)));
	}

	@Test
	public void whoStage_shouldConvertConceptToInteger() {
		Assert.assertNull(EmrUtils.whoStage(Dictionary.getConcept(Dictionary.CD4_COUNT)));
		Assert.assertEquals(new Integer(1), EmrUtils.whoStage(Dictionary.getConcept(Dictionary.WHO_STAGE_1_PEDS)));
		Assert.assertEquals(new Integer(2), EmrUtils.whoStage(Dictionary.getConcept(Dictionary.WHO_STAGE_2_ADULT)));
		Assert.assertEquals(new Integer(3), EmrUtils.whoStage(Dictionary.getConcept(Dictionary.WHO_STAGE_3_PEDS)));
		Assert.assertEquals(new Integer(4), EmrUtils.whoStage(Dictionary.getConcept(Dictionary.WHO_STAGE_4_ADULT)));
	}

	/**
	 * @see EmrUtils#parseCsv(String)
	 */
	@Test
	public void parseCsv_shouldParseCsv() {
		Assert.assertThat(EmrUtils.parseCsv("test"), contains("test"));
		Assert.assertThat(EmrUtils.parseCsv("1, 2,test"), contains("1", "2", "test"));
		Assert.assertThat(EmrUtils.parseCsv("1, 2,, , 3"), contains("1", "2", "3"));
	}

	/**
	 * @see EmrUtils#parseConcepts(String)
	 */
	@Test
	public void parseConcepts_shouldParseListCorrectly() {
		// Empty list
		List<Concept> concepts = EmrUtils.parseConcepts("");
		Assert.assertEquals(0, concepts.size());

		// No spaces
		concepts = EmrUtils.parseConcepts("5497,730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA,5356");
		Assert.assertEquals(3, concepts.size());
		Assert.assertEquals(Dictionary.getConcept("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), concepts.get(0));
		Assert.assertEquals(Dictionary.getConcept("730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), concepts.get(1));
		Assert.assertEquals(Dictionary.getConcept("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), concepts.get(2));

		// Some spaces
		concepts = EmrUtils.parseConcepts(" 5497,  730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\t , 5356   \t");
		Assert.assertEquals(3, concepts.size());
		Assert.assertEquals(Dictionary.getConcept("5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), concepts.get(0));
		Assert.assertEquals(Dictionary.getConcept("730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), concepts.get(1));
		Assert.assertEquals(Dictionary.getConcept("5356AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"), concepts.get(2));
	}

	/**
	 * @see EmrUtils#lastObs(org.openmrs.Person, org.openmrs.Concept)
	 */
	@Test
	public void lastObs_shouldFindLastObsWithConcept() {
		Concept cd4 = Dictionary.getConcept(Dictionary.CD4_COUNT);
		Patient patient = TestUtils.getPatient(7);

		TestUtils.saveObs(patient, cd4, 123.0, TestUtils.date(2012, 1, 1));
		TestUtils.saveObs(patient, cd4, 234.0, TestUtils.date(2012, 1, 2));
		Obs obs = TestUtils.saveObs(patient, cd4, 345.0, TestUtils.date(2012, 1, 3));
		TestUtils.saveObs(patient, Dictionary.getConcept(Dictionary.CD4_PERCENT), 50.0, TestUtils.date(2012, 1, 31)); // Wrong concept

		Assert.assertThat(EmrUtils.lastObs(patient, cd4), is(obs));
	}

	/**
	 * @see EmrUtils#lastEncounter(org.openmrs.Patient, org.openmrs.EncounterType)
	 */
	@Test
	public void lastEncounter_shouldFindLastEncounterWithType() {
		Patient patient = TestUtils.getPatient(6);
		EncounterType triageEncType = MetadataUtils.getEncounterType(CommonMetadata._EncounterType.TRIAGE);
		EncounterType tbScreenEncType = MetadataUtils.getEncounterType(TbMetadata._EncounterType.TB_SCREENING);

		// Test with no saved encounters
		Assert.assertNull(EmrUtils.lastEncounter(patient, tbScreenEncType));

		Encounter enc1 = TestUtils.saveEncounter(patient, triageEncType, TestUtils.date(2012, 3, 1));
		Encounter enc2 = TestUtils.saveEncounter(patient, tbScreenEncType, TestUtils.date(2012, 2, 1));
		Encounter enc3 = TestUtils.saveEncounter(patient, tbScreenEncType, TestUtils.date(2012, 1, 1));

		Assert.assertEquals(enc2, EmrUtils.lastEncounter(patient, tbScreenEncType));
	}
}