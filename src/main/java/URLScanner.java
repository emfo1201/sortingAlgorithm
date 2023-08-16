import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLScanner {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]");

    public static void main(String[] args) {
        String folderPath = "C:/folder/path/"; // Replace with folder path
        scanUrlsInFolder(folderPath);
    }

    private static void scanUrlsInFolder(String folderPath) {
        Observable.fromIterable(getFilesInFolder(folderPath))
                .flatMap(URLScanner::scanUrlsInFile)
                .observeOn(Schedulers.io())
                .subscribe(
                        outputPath -> System.out.println("Saved URLs to " + outputPath),
                        URLScanner::onError,
                        URLScanner::onComplete
                );
    }

    private static List<Path> getFilesInFolder(String folderPath) {
        try {
            List<Path> fileList = new ArrayList<>();
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .forEach(fileList::add);
            return fileList;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static Observable<Path> scanUrlsInFile(Path filePath) {
        return Observable.create(emitter -> {
            List<String> urls = new ArrayList<>();
            try {
                Files.lines(filePath)
                        .flatMap(line -> extractUrls(line).stream())
                        .forEach(urls::add);
                Path outputPath = saveUrlsToFile(urls, filePath.getFileName().toString());
                emitter.onNext(outputPath);
                emitter.onComplete();
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }

    private static List<String> extractUrls(String line) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(line);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    private static Path saveUrlsToFile(List<String> urlList, String fileName) throws IOException {
        if (urlList.isEmpty()) {
            return null;
        }

        String outputFileName = "urls_" + fileName;
        Path outputPath = Paths.get(fileName).resolveSibling(outputFileName);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (String url : urlList) {
                writer.write(url);
                writer.newLine();
            }
        }
        return outputPath;
    }

    private static void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private static void onComplete() {
        System.out.println("URL scanning completed.");
    }
}


