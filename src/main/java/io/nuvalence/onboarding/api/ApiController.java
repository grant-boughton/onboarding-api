package io.nuvalence.onboarding.api;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@RestController
public class ApiController {

    private AmazonS3 s3Client;
    private DynamoDB dynamoClient;

    public static void main(String[] args) {
        SpringApplication.run(ApiController.class, args);
    }

    @Autowired
    public ApiController(AmazonS3 amazonS3, DynamoDB dynamoDB){
        this.s3Client = amazonS3;
        this.dynamoClient = dynamoDB;
    }

    @RequestMapping("/")
    public String home(){
        return "This is a home page";
    }

    @CrossOrigin
    @RequestMapping("/list")
    public List<String> list(){
        ArrayList<String> output = new ArrayList<String>();
        Table table = this.dynamoClient.getTable("grant-onboarding");

        for (Item item : table.scan()) {
            output.add(item.toJSON());
        }
        return output;
    }

    @CrossOrigin
    @RequestMapping("/upload/{bucketName}/{fileName}")
    public String upload(@PathVariable("bucketName") String bucketName, @PathVariable("fileName") String fileName, @RequestParam String user){

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 30;
        //Set expiration to 30 seconds from current time
        expiration.setTime(expTimeMillis);
        fileName = "users/"+user+"/"+fileName;

        GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(bucketName,fileName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        URL url = this.s3Client.generatePresignedUrl(urlRequest);
        return url.toString();
    }
}