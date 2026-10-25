package com.mechalikh.pureedgesim.scenariomanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.mechalikh.pureedgesim.distribution.BoundedDistribution;
import com.mechalikh.pureedgesim.distribution.ConstantDistribution;
import com.mechalikh.pureedgesim.distribution.Distribution;
import com.mechalikh.pureedgesim.distribution.NormalDistribution;
import com.mechalikh.pureedgesim.distribution.UniformDistribution;

/**
 * Tests of {@link DistributionReader}, the bridge between the XML syntax and the
 * distribution objects.
 */
class DistributionReaderTest {

	private Element element(String xml) throws Exception {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(
				new ByteArrayInputStream(("<root>" + xml + "</root>").getBytes(StandardCharsets.UTF_8)));
		return (Element) document.getDocumentElement().getFirstChild();
	}

	@Test
	void aPlainNumberBecomesAConstantDistribution() throws Exception {
		Distribution distribution = DistributionReader.read(element("<size>25</size>"), "\"size\"");
		assertInstanceOf(ConstantDistribution.class, distribution);
		assertEquals(25.0, distribution.sample(), 0.0001);
		assertEquals(25.0, distribution.mean(), 0.0001);
		assertEquals("constant", distribution.getName());
	}

	@Test
	void surroundingWhitespaceIsIgnored() throws Exception {
		assertEquals(7.0, DistributionReader.read(element("<size>  7  </size>"), "\"size\"").mean(), 0.0001);
	}

	@Test
	void isDistributionOnlyDetectsTheKeywordAttribute() throws Exception {
		assertFalse(DistributionReader.isDistribution(null));
		assertFalse(DistributionReader.isDistribution(element("<size>25</size>")));
		assertTrue(DistributionReader.isDistribution(element("<size distribution=\"uniform\" min=\"1\" max=\"2\"/>")));
	}

	@Test
	void aDistributionElementIsBuiltFromItsAttributes() throws Exception {
		Distribution distribution = DistributionReader
				.read(element("<size distribution=\"uniform\" min=\"10\" max=\"20\"/>"), "\"size\"");
		assertInstanceOf(UniformDistribution.class, distribution);
		assertEquals(15.0, distribution.mean(), 0.0001);
		for (int i = 0; i < 200; i++) {
			double value = distribution.sample();
			assertTrue(value >= 10 && value <= 20, "uniform draw " + value + " left its support");
		}
	}

	@Test
	void readMeanReportsTheDistributionMeanWithoutSamplingIt() throws Exception {
		assertEquals(10.0,
				DistributionReader.readMean(element("<length distribution=\"normal\" mean=\"10\" std=\"2\"/>"),
						"\"length\""),
				0.0001);
	}

	@Test
	void minAndMaxBoundAnyDistribution() throws Exception {
		Distribution distribution = DistributionReader.read(
				element("<size distribution=\"normal\" mean=\"10\" std=\"10\" min=\"0\" max=\"20\"/>"), "\"size\"");
		assertInstanceOf(BoundedDistribution.class, distribution);
		assertFalse(distribution instanceof NormalDistribution, "an unbounded support must be wrapped, not returned as is");
		for (int i = 0; i < 500; i++) {
			double value = distribution.sample();
			assertTrue(value >= 0 && value <= 20, "bounded draw " + value + " left the range");
		}
	}

	@Test
	void aMissingElementIsReportedAsMissing() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionReader.read(null, "\"request_size\" of \"application\""));
		assertTrue(error.getMessage().contains("\"request_size\" of \"application\" is missing"), error.getMessage());
	}

	@Test
	void anEmptyElementExplainsBothAcceptedSyntaxes() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionReader.read(element("<size></size>"), "\"size\""));
		assertTrue(error.getMessage().contains("is empty"), error.getMessage());
		assertTrue(error.getMessage().contains("distribution"), error.getMessage());
	}

	@Test
	void aNonNumericValueIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> DistributionReader.read(element("<size>big</size>"), "\"size\""));
		assertTrue(error.getMessage().contains("must be a number or a distribution"), error.getMessage());
		assertTrue(error.getMessage().contains("found \"big\""), error.getMessage());
	}

	@Test
	void anUnknownDistributionNameIsRejected() {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> DistributionReader
				.read(element("<size distribution=\"cauchy\" min=\"1\" max=\"2\"/>"), "\"size\""));
		assertTrue(error.getMessage().contains("is not supported"), error.getMessage());
	}

	@Test
	void attributesKeepTheirNamesInLowerCaseAndDropTheKeyword() throws Exception {
		Map<String, String> attributes = DistributionReader
				.attributes(element("<size distribution=\"uniform\" MiN=\"1\" MAX=\"9\"/>"));
		assertEquals(2, attributes.size());
		assertEquals("1", attributes.get("min"));
		assertEquals("9", attributes.get("max"));
		assertFalse(attributes.containsKey("distribution"));
	}

	@Test
	void theKeywordAttributeNameIsSharedWithTheFactory() {
		assertEquals("distribution", DistributionReader.NAME_ATTRIBUTE);
	}
}
