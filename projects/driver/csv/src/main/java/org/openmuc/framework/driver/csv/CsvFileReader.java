package org.openmuc.framework.driver.csv;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.driver.spi.ConnectionException;

import com.univocity.parsers.common.processor.ColumnProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * Class to parse the CSV file into a map of column names and their respective list of values
 */
public class CsvFileReader {

    HashMap<String, List<Double>> data;

    public static Map<String, List<String>> readCsvFile(String fileName) throws ConnectionException {

        // https://github.com/uniVocity/univocity-parsers#reading-columns-instead-of-rows

        ColumnProcessor processor = new ColumnProcessor();
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.getFormat().setLineSeparator("\n");
        parserSettings.setHeaderExtractionEnabled(true);
        parserSettings.setProcessor(processor);

        CsvParser parser = new CsvParser(parserSettings);
        FileReader reader;

        try {
            reader = new FileReader(fileName);
            parser.parse(reader);
            reader.close();
        } catch (IOException e) {
            throw new ConnectionException("Unable to parse file.", e);
        }

        // Finally, we can get the column values:
        Map<String, List<String>> columnValues = processor.getColumnValuesAsMapOfNames();

        return columnValues;

    }

}
