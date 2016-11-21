package org.openmuc.framework.driver.csv;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.univocity.parsers.common.processor.ColumnProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class CsvFileReader {

    HashMap<String, List<Double>> data;

    public static Map<String, List<String>> readCsvFile(String fileName) throws IOException {

        // https://github.com/uniVocity/univocity-parsers#reading-columns-instead-of-rows

        ColumnProcessor processor = new ColumnProcessor();
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.getFormat().setLineSeparator("\n");
        parserSettings.setHeaderExtractionEnabled(true);
        parserSettings.setProcessor(processor);

        CsvParser parser = new CsvParser(parserSettings);
        FileReader reader = new FileReader(fileName);
        parser.parse(reader);

        // Finally, we can get the column values:
        Map<String, List<String>> columnValues = processor.getColumnValuesAsMapOfNames();

        reader.close();

        return columnValues;

        // // print keys

        //
        // // print value of key
        //
        // List<String> values = columnValues.get("unixtimestamp");
        // for (String value : values) {
        // System.out.println(value);
        // }

    }

    // List<String> lines;
    //
    // public CsvFileReader(String filepath) throws ConnectionException {
    //
    // if (!filepath.endsWith(".csv")) {
    // throw new ConnectionException("Wrong file type. File must be a CSV file (*.csv)");
    // }
    //
    // File file = new File(filepath);
    // if (file.canRead()) {
    // lines = getFileAsStringList(file.toPath());
    // }
    // else {
    // throw new ConnectionException("File (" + file.getAbsolutePath() + ") is not readable.");
    // }
    // }
    //
    // private List<String> getFileAsStringList(Path path) throws ConnectionException {
    // List<String> returnValue = null;
    // try {
    // returnValue = Files.readAllLines(path, Charset.forName("UTF-8"));
    // } catch (IOException e) {
    // throw new ConnectionException("IoExcecption by read File (" + path.getFileName() + ")\n" + e.getMessage());
    // }
    // return returnValue;
    // }
    //
    // public String[] getColumnNames() throws ConnectionException {
    //
    // String[] columnNames = null;
    //
    // Iterator<String> iterator = lines.iterator();
    //
    // while (iterator.hasNext()) {
    // String line = iterator.next();
    // if (!line.startsWith("#")) {
    // columnNames = line.split(";");
    // if (columnNames.length <= 0) {
    // throw new ConnectionException("Unable to parse CSV column names");
    // }
    // break;
    // }
    // }
    //
    // if (columnNames == null) {
    // throw new ConnectionException("Unable to parse CSV column names");
    // }
    // return columnNames;
    //
    // }

}
