package ca.bc.gov.hlth.iam.dataloader.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import ca.bc.gov.hlth.iam.dataloader.model.csv.UserData;

public class CSVFileService {

	private static final Logger logger = LoggerFactory.getLogger(CSVFileService.class);

	public List<UserData> extractFileInfo(String fileLoction) throws Exception {
		logger.info("Extracting data from file at: {}", fileLoction);
		
		Path path = Paths.get(fileLoction);
		
		List<UserData> csvList = new ArrayList<>();

		try (Reader reader = Files.newBufferedReader(path)) {
			CsvToBean<UserData> builder = new CsvToBeanBuilder<UserData>(reader)
					.withType(UserData.class)
					.build();

			csvList = (builder.parse());
		} catch (Exception e) {
			logger.error("User data could not be extracted due to: {}", e.getMessage());
			throw e;
		}
	    
	    logger.info("Extracted records: {}", csvList.size());
	    
	    return csvList;
	}

	public void generateCsvFile(List<UserRepresentation> users, String filePath) throws IOException {
		try (FileWriter fileWriter = new FileWriter(filePath);
			 PrintWriter printWriter = new PrintWriter(fileWriter)) {

			printWriter.println("USERNAME,ID");

			for (UserRepresentation user : users) {
				printWriter.println(user.getUsername() + "," + user.getId());
			}
			logger.info("File created at this location: {}", filePath);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
