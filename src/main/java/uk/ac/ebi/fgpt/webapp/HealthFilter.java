package uk.ac.ebi.fgpt.webapp;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A health check filter that complies with EBI E.S. guidelines about how to filter health requests to your webapp to
 * prevent excessive logging of automated checks.
 * <p/>
 * Web applications deployed at one of the EBI London Datacentres are monitored by a load balancer.  This load balancer
 * sends a request every three seconds, and the datacentre tomcats are set up to log requests in access log files (using
 * a valve).  By setting this HealthFilter in your webapp, health check requests will be filtered and the log files will
 * not be overwhelmed.
 * <p/>
 * For more information, see <a href="http://wwwint.ebi.ac.uk/es/web-administration/Public/load-balancer-monitor>http://wwwint.ebi.ac.uk/es/web-administration/Public/load-balancer-monitor</a>
 *
 * @author Tony Burdett
 * @date 13/10/11
 */
public class HealthFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        servletRequest.setAttribute("health", true);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }
}