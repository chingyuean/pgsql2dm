package com.tool.pgsql2dm.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import com.tool.pgsql2dm.config.ConvertorConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Ching
 * @mail : chingyuean@
 * @description :
 * @since : 2/7/2022,10:17 AM,Monday
 **/
@Slf4j
@Component
public class Processor implements CommandLineRunner {
    public static final String UNDERSCORE = "_";
    public static final String DOT = ".";
    @Resource
    private ConvertorConfig config;

    @Override
    public void run(String... args) throws Exception {
        // sql file read
        String filePath = config.getFilePath();
        if (!FileUtil.exist(filePath)) {
            log.error("No File Found!");
            return;
        }
        File file = FileUtil.file(filePath);
        if (file.isDirectory()) {
            // get sql files
            File[] sqlFiles = file.listFiles((dir, name) -> {
                name = name.toLowerCase(Locale.ROOT);
                return name.endsWith(".sql");
            });
            // do convert
            for (File sqlFile : sqlFiles) {
                convert(sqlFile);
            }
        } else if (file.isFile()) {
            convert(file);
        } else {
            log.error("Path Type Not Recognized!");
        }
    }

    /**
     * convert pgsql 2 dmsql
     * @param sqlFile
     */
    private void convert(File sqlFile) {
        if (!sqlFile.isFile()){
            log.error("Failed to convert, not a file!");
            return;
        };
        String fileName = sqlFile.getName();
        String[] fileNameAndType = fileName.split("\\.");
        String outputName = fileNameAndType[0] + UNDERSCORE + "dm" + DOT + "sql";
        // read and convert
        FileReader fileReader = new FileReader(sqlFile);
        String sqlText = fileReader.readString();
        // simple replace
        sqlText = sqlText.replaceAll("\\s(int|INT)\\d", " INTEGER")
                .replaceAll("\\s(bool|BOOL)"," BIT")
                .replaceAll("((DEFAULT|default)[\\s](false|FALSE))","DEFAULT 0")
                .replaceAll("('f',)","0,")
                .replaceAll("('t',)","1,");
        // output discarded
        String discardedOutputName = fileNameAndType[0] + UNDERSCORE + "discarded" + DOT + "sql";
        File discardFile = FileUtil.file(discardedOutputName);
        sqlText = removeAndOutput("((ALTER)[\\s](TABLE)[^;]+(COLLATE)[^;]+(USING)[^;]+;)",sqlText,discardFile);
        sqlText = removeAndOutput("(COLLATE[\\s]+\"pg_catalog\"\\.\"default\")",sqlText,discardFile);
        sqlText = removeAndOutput("(::character[\\s]varying)",sqlText,discardFile);
        sqlText = removeAndOutput("((CREATE|create)[\\s]+(INDEX|index)[\\s]+[^;]+;)",sqlText,discardFile);

        // write file
        FileWriter fileWriter = FileWriter.create(FileUtil.file(outputName));
        fileWriter.write(sqlText);
    }

    /**
     * output the removed line to file
     * @param regex
     * @param source
     * @param outputFile
     * @return
     */
    private String removeAndOutput(String regex,String source, File outputFile) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);

        FileWriter writer = FileWriter.create(outputFile);

        while (matcher.find()) {
            String matches = matcher.group();
            writer.append(matches + "\n");
        }
        return  source.replaceAll(regex,"");
    }

}
