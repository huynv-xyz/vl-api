package vsf.cms;

import io.micronaut.context.ApplicationContext;
import org.mindrot.jbcrypt.BCrypt;


public class ContentTest {
    public static void main(String[] args) throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run()) {
            System.out.println(BCrypt.hashpw("123456", BCrypt.gensalt()));



        }
    }
}

