/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package com.github.jknack.handlebars.internal;

import java.io.IOException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.ToolErrorReporter;

import com.github.jknack.handlebars.Template;

/**
 * Convert a template to JavaScript template (a.k.a precompiled template). Compilation is done by
 * handlebars.js and a JS Engine. For now, default and unique engine is Rhino.
 *
 * @author edgar
 *
 */
enum JSEngine {

  /**
   * The default JS Engine.
   */
  RHINO {
    @Override
    public String toJavaScript(final Template template) {
      Context ctx = null;
      try {
        ctx = newContext();

        Scriptable scope = newScope(ctx);
        scope.put("template", scope, template.text());

        String js = "Handlebars.precompile(template);";
        Object precompiled = ctx.evaluateString(scope, js, template.toString(), 1,
            null);

        return (String) precompiled;
      } finally {
        if (ctx != null) {
          org.mozilla.javascript.Context.exit();
        }
      }
    }

    /**
     * Creates a new scope where handlebars.js is present.
     *
     * @param ctx A rhino context.
     * @return A new scope where handlebars.js is present.
     */
    private Scriptable newScope(final Context ctx) {
      Scriptable sharedScope = sharedScope(ctx);
      Scriptable scope = ctx.newObject(sharedScope);
      scope.setParentScope(null);
      scope.setPrototype(sharedScope);

      return scope;
    }

    /**
     * Creates a new Rhino Context.
     *
     * @return A Rhino Context.
     */
    private Context newContext() {
      Context ctx = Context.enter();
      ctx.setOptimizationLevel(-1);
      ctx.setErrorReporter(new ToolErrorReporter(false));
      ctx.setLanguageVersion(Context.VERSION_1_8);
      return ctx;
    }

    /**
     * Creates a initialize the handlebars.js scope.
     *
     * @param ctx A rhino context.
     * @return A handlebars.js scope. Shared between executions.
     */
    private Scriptable sharedScope(final Context ctx) {
      ScriptableObject sharedScope = ctx.initStandardObjects();
      ctx.evaluateString(sharedScope, handlebarsScript(HBS_FILE), HBS_FILE, 1, null);
      return sharedScope;
    }

    /**
     * Load the handlebars.js file from the given location.
     *
     * @param location The handlebars.js location.
     * @return The resource content.
     */
    private String handlebarsScript(final String location) {
      try {
        return Files.read(location);
      } catch (IOException ex) {
        throw new IllegalArgumentException("Unable to read file: " + location, ex);
      }
    }
  };

  /**
   * Handlerbars.js version.
   */
  private static final String HBS_FILE = "/handlebars-v1.3.0.js";

  /**
   * Convert this template to JavaScript template (a.k.a precompiled template). Compilation is done
   * by handlebars.js and a JS Engine (usually Rhino).
   *
   * @param template The template to convert.
   * @return A pre-compiled JavaScript version of this template.
   */
  public abstract String toJavaScript(Template template);
}