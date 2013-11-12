package io.wunderboss.ruby.rack;

import io.wunderboss.Application;
import io.wunderboss.Component;
import io.wunderboss.ComponentInstance;
import io.wunderboss.Options;
import io.wunderboss.ruby.RuntimeHelper;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashMap;
import java.util.Map;

public class RackComponent extends Component{
    @Override
    public String[] getLanguageDependencies() {
        return new String[]{"ruby"};
    }

    @Override
    public String[] getComponentDependencies() {
        return new String[]{"servlet"};
    }

    @Override
    public void boot() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void configure(Options options) {
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        String context = options.getString("context", "/");
        String root = options.getString("root", ".");
        String staticDirectory = options.getString("static_dir", root + "/public");

        String rackScript = "require 'rack'\n" +
                "app, _ = Rack::Builder.parse_file(File.join('" + root + "', 'config.ru'))\n" +
                "app\n";
        IRubyObject rackApplication = RuntimeHelper.evalScriptlet(getRuntime(application), rackScript, false);

        Map<String, Object> servletContextAttributes = new HashMap<>();
        servletContextAttributes.put("rack_application", rackApplication);

        Options servletOptions = new Options();
        servletOptions.put("context", context);
        servletOptions.put("static_dir", staticDirectory);
        servletOptions.put("servlet_class", RackServlet.class);
        servletOptions.put("context_attributes", servletContextAttributes);
        ComponentInstance servlet = application.start("servlet", servletOptions);

        Options instanceOptions = new Options();
        instanceOptions.put("servlet", servlet);
        return new ComponentInstance(this, instanceOptions);
    }

    @Override
    public void stop(ComponentInstance instance) {
        ComponentInstance servlet = (ComponentInstance) instance.getOptions().get("servlet");
        servlet.stop();
    }

    private Ruby getRuntime(Application application) {
        return (Ruby) application.getRuntime();
    }
}
