
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoogleReport {
	private static final String APPLICATION_NAME = "Test Reporting via in Google Sheets API";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = GoogleReport.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}
	private Params params = new Params();

	public void updateReport(String tc_id, String tc_status) throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		final String range = params.TESTCASE_SHEETNAME+"!"+params.TESTCASEID_COLUMN+"1:"+params.TESTCASEID_COLUMN;
		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		ValueRange response = service.spreadsheets().values().get(params.SPREADSHEETID, range).execute();
		List<List<Object>> values = response.getValues();
		int rownum = 0;
		int count = 0;
		if (values == null || values.isEmpty()) {
		} else {
			for (List row : values) {
				row.add("");
//                System.out.println(row.size());
				if (row.get(0).equals(tc_id)) {
					rownum = count + 1;
				}
				count++;
			}
		}
//        System.out.println(rownum);

		if (count == 0)
			System.out.println("No data found in TestCaseId column in Google Sheet. Please check the configuration in Params.java");
		else if (rownum == 0)
			System.out.println("Unable to find a test case with id "+tc_id);
		else {
			ValueRange requestBody = new ValueRange().setValues(Arrays.asList(Arrays.asList(tc_status)));
			Sheets.Spreadsheets.Values.Update request = service.spreadsheets().values().update(params.SPREADSHEETID,
					params.TESTCASE_SHEETNAME+"!"+params.STATUS_COLUMN + rownum, requestBody);
			request.setValueInputOption("RAW").execute();

			System.out.println("Status of "+tc_id+" updated as "+tc_status);
		}
	}
}
