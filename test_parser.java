import org.joshsim.pipeline.job.config.JobVariationParser;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJob;
import java.util.List;

public class test_parser {
    public static void main(String[] args) {
        JobVariationParser parser = new JobVariationParser();
        JoshJobBuilder builder = new JoshJobBuilder().setReplicates(1);
        String[] dataFiles = {"example.jshc=test_data/example_1.jshc,other.jshd=test_data/other_1.jshd"};
        
        try {
            List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
            System.out.println("Results count: " + results.size());
            
            for (int i = 0; i < results.size(); i++) {
                JoshJob job = results.get(i).build();
                System.out.println("Job " + i + ":");
                for (String fileName : job.getFileNames()) {
                    System.out.println("  " + fileName + " -> " + job.getFilePath(fileName));
                }
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
