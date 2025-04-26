import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CORSFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        response.setContentType("application/json");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        ((HttpServletResponse) response).addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup logic if necessary
    }
}
