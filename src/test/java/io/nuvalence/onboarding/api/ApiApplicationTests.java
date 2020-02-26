package io.nuvalence.onboarding.api;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.Assert;

import java.net.URL;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AmazonS3 s3Client;

	@MockBean
	private DynamoDB dynamoClient;

	@MockBean
	private Table mockTable;

	@Test
	void contextLoads() {
	}

	@Test
	public void testHomePath() throws Exception {
		this.mockMvc.perform(get("/")).andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("This is a home page")));
	}

	@Test
	public void testItemList() throws Exception {

		List<Map<String, AttributeValue>> mockItems = new ArrayList<Map<String, AttributeValue>>(){{
			add(new HashMap<String,AttributeValue>(){{
				put("attribute", new AttributeValue("value"));
			}});
			add(new HashMap<String,AttributeValue>(){{
				put("attribute2", new AttributeValue("value2"));
			}});
		}};

		ScanResult mockScanResult = new ScanResult().withItems(mockItems);

		Mockito.when(dynamoClient.getTable("grant-onboarding")).thenReturn(mockTable);
		//need to return ScanCollection, not sure how, got lost in the interfaces
		//when(mockTable.scan()).thenReturn(new ScanOutcome(mockScanResult));
	}

	@Test
	public void testUploadPath() throws Exception {

		ApiController testController = new ApiController(s3Client, dynamoClient);
		ArgumentCaptor<GeneratePresignedUrlRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		String userName = "testUser";
		String fileName = "test.jpg";
		String bucketName = "bucket";

		GeneratePresignedUrlRequest expectedRequest = new GeneratePresignedUrlRequest(bucketName, "users/"+userName+"/"+fileName)
				.withMethod(HttpMethod.PUT);

		//Using mockito.any() because impossible to know what expiration will be on actual request
		Mockito.when(s3Client.generatePresignedUrl(Mockito.any())).thenReturn(new URL("https://google.com"));

		testController.upload(bucketName,fileName,userName);
		Mockito.verify(s3Client).generatePresignedUrl(requestArgumentCaptor.capture());
		GeneratePresignedUrlRequest actualRequest = requestArgumentCaptor.getValue();

		//verify correct request information, excluding expiration
		Assert.assertEquals(expectedRequest.getBucketName(), actualRequest.getBucketName());
		Assert.assertEquals(expectedRequest.getKey(), actualRequest.getKey());
		Assert.assertEquals(expectedRequest.getMethod(), actualRequest.getMethod());

	}
}
