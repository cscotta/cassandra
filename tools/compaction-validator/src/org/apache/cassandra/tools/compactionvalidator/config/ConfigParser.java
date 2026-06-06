/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.tools.compactionvalidator.config;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Loads {@link RunConfig} from a YAML file.
 *
 * <p>YAML keys use {@code snake_case} (matching cassandra.yaml conventions);
 * Java fields use {@code camelCase}. SnakeYAML's default property-mapper does
 * a case-fold but doesn't translate the underscore-separated form, so we use
 * {@link BeanAccess#FIELD} which lets snake_case keys land directly on
 * snake-named setters (e.g. {@code setTarget_bytes}). Each config class
 * defines those alias setters explicitly.
 *
 * <p>Validation happens via {@link RunConfig#validate} after construction so
 * type errors come out as a clear "config X is wrong" message before any
 * heavy work begins.
 */
public final class ConfigParser
{
    private ConfigParser() {}

    /**
     * Parses the file at {@code path}. Throws {@link IllegalArgumentException}
     * with a user-facing message on parse error or validation failure.
     */
    public static RunConfig load(File path) throws IOException
    {
        if (path == null || !path.isFile())
            throw new IllegalArgumentException("config file does not exist: " + path);

        // Load as a Map first so we can hand-translate snake_case keys to camelCase
        // properties before SnakeYAML does its bean binding. This is more permissive
        // and avoids fighting SnakeYAML's property-name resolver.
        try (Reader reader = Files.newBufferedReader(path.toPath(), StandardCharsets.UTF_8))
        {
            LoaderOptions opts = new LoaderOptions();
            opts.setAllowDuplicateKeys(false);
            Yaml raw = new Yaml(opts);
            Object root = raw.load(reader);
            if (!(root instanceof Map))
                throw new IllegalArgumentException("config root must be a YAML mapping; got " + classNameOf(root));
            @SuppressWarnings("unchecked")
            Map<String, Object> rootMap = (Map<String, Object>) root;
            RunConfig cfg = bindRunConfig(rootMap);
            cfg.validate();
            return cfg;
        }
        catch (YAMLException ye)
        {
            throw new IllegalArgumentException("YAML parse error in " + path + ": " + ye.getMessage(), ye);
        }
    }

    // -------------------------------------------------------------------------
    // Hand-rolled binders. Trades a few lines of boilerplate for a much better
    // error message surface than reflective binding's "no setter for foo" stacks.
    // -------------------------------------------------------------------------

    private static RunConfig bindRunConfig(Map<String, Object> m)
    {
        RunConfig cfg = new RunConfig();
        if (m.containsKey("version"))
            cfg.version = asInt(m.get("version"), "version");
        if (m.containsKey("run"))
            cfg.run = bindRunSettings(asMap(m.get("run"), "run"));
        if (m.containsKey("schema"))
            cfg.schema = bindSchemaOverrides(asMap(m.get("schema"), "schema"));
        if (m.containsKey("comparison"))
            cfg.comparison = bindComparison(asMap(m.get("comparison"), "comparison"));
        rejectUnknownKeys(m, "<root>", "version", "run", "schema", "comparison");
        return cfg;
    }

    private static RunSettings bindRunSettings(Map<String, Object> m)
    {
        RunSettings rs = new RunSettings();
        if (m.containsKey("target_bytes")) rs.targetBytes       = asString(m.get("target_bytes"), "run.target_bytes");
        if (m.containsKey("datagen_threads")) rs.datagenThreads = asInt(m.get("datagen_threads"), "run.datagen_threads");
        if (m.containsKey("compaction_threads")) rs.compactionThreads = asInt(m.get("compaction_threads"), "run.compaction_threads");
        if (m.containsKey("validation_threads")) rs.validationThreads = asInt(m.get("validation_threads"), "run.validation_threads");
        if (m.containsKey("max_runs")) rs.maxRuns = asInt(m.get("max_runs"), "run.max_runs");
        if (m.containsKey("no_cleanup")) rs.noCleanup = asBool(m.get("no_cleanup"), "run.no_cleanup");
        if (m.containsKey("no_ui")) rs.noUi = asBool(m.get("no_ui"), "run.no_ui");
        if (m.containsKey("ignore_errata"))
        {
            Object v = m.get("ignore_errata");
            if (v == null) rs.ignoreErrata = new ArrayList<>();
            else if (v instanceof List)
            {
                rs.ignoreErrata = new ArrayList<>();
                for (Object o : (List<?>) v) rs.ignoreErrata.add(String.valueOf(o));
            }
            else if (v instanceof String)
            {
                // Allow comma-separated string for parity with the old --ignore-errata CLI form
                rs.ignoreErrata = new ArrayList<>();
                for (String t : ((String) v).split(","))
                    if (!t.trim().isEmpty()) rs.ignoreErrata.add(t.trim());
            }
            else
                throw new IllegalArgumentException("run.ignore_errata must be a list or comma-separated string");
        }
        rejectUnknownKeys(m, "run",
                          "target_bytes", "datagen_threads", "compaction_threads",
                          "validation_threads", "max_runs", "no_cleanup", "no_ui", "ignore_errata");
        return rs;
    }

    private static SchemaOverrides bindSchemaOverrides(Map<String, Object> m)
    {
        SchemaOverrides s = new SchemaOverrides();
        if (m.containsKey("partitioner")) s.partitioner = asString(m.get("partitioner"), "schema.partitioner");
        rejectUnknownKeys(m, "schema", "partitioner");
        return s;
    }

    private static ComparisonConfig bindComparison(Map<String, Object> m)
    {
        ComparisonConfig c = new ComparisonConfig();
        if (m.containsKey("control"))    c.control    = bindSide(asMap(m.get("control"), "comparison.control"), "comparison.control");
        if (m.containsKey("experiment")) c.experiment = bindSide(asMap(m.get("experiment"), "comparison.experiment"), "comparison.experiment");
        rejectUnknownKeys(m, "comparison", "control", "experiment");
        return c;
    }

    private static SideConfig bindSide(Map<String, Object> m, String path)
    {
        SideConfig s = new SideConfig();
        if (m.containsKey("name"))        s.name        = asString(m.get("name"), path + ".name");
        if (m.containsKey("pipeline"))    s.pipeline    = asString(m.get("pipeline"), path + ".pipeline");
        if (m.containsKey("compaction"))  s.compaction  = bindCompaction(asMap(m.get("compaction"), path + ".compaction"), path + ".compaction");
        if (m.containsKey("compression")) s.compression = bindCompression(asMap(m.get("compression"), path + ".compression"), path + ".compression");
        if (m.containsKey("io_mode"))     s.ioMode      = asString(m.get("io_mode"), path + ".io_mode");
        if (m.containsKey("format"))      s.format      = asString(m.get("format"), path + ".format");
        rejectUnknownKeys(m, path, "name", "pipeline", "compaction", "compression", "io_mode", "format");
        return s;
    }

    private static CompactionSpec bindCompaction(Map<String, Object> m, String path)
    {
        CompactionSpec c = new CompactionSpec();
        if (m.containsKey("class"))   c.className = asString(m.get("class"), path + ".class");
        if (m.containsKey("options"))
        {
            Map<String, Object> opts = asMap(m.get("options"), path + ".options");
            for (Map.Entry<String, Object> e : opts.entrySet())
                c.options.put(e.getKey(), String.valueOf(e.getValue()));
        }
        rejectUnknownKeys(m, path, "class", "options");
        return c;
    }

    private static CompressionSpec bindCompression(Map<String, Object> m, String path)
    {
        CompressionSpec c = new CompressionSpec();
        if (m.containsKey("class"))   c.className = asString(m.get("class"), path + ".class");
        // Compression has both a flat shape (class + chunk_length_in_kb at the top level)
        // and a nested shape (class + options: { ... }) — accept either to keep the YAML
        // ergonomic. Flat sibling keys other than 'class' fold into the options map.
        if (m.containsKey("options"))
        {
            Map<String, Object> opts = asMap(m.get("options"), path + ".options");
            for (Map.Entry<String, Object> e : opts.entrySet())
                c.options.put(e.getKey(), String.valueOf(e.getValue()));
        }
        for (Map.Entry<String, Object> e : m.entrySet())
        {
            if ("class".equals(e.getKey()) || "options".equals(e.getKey())) continue;
            c.options.put(e.getKey(), String.valueOf(e.getValue()));
        }
        return c;
    }

    // -------------------------------------------------------------------------
    // Type coercion helpers — produce friendly error messages instead of
    // SnakeYAML's stacks.
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object v, String path)
    {
        if (v == null)
            return new LinkedHashMap<>();
        if (!(v instanceof Map))
            throw new IllegalArgumentException(path + " must be a YAML mapping; got " + classNameOf(v));
        return (Map<String, Object>) v;
    }

    private static String asString(Object v, String path)
    {
        if (v == null) return null;
        if (v instanceof String) return (String) v;
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        throw new IllegalArgumentException(path + " must be a string; got " + classNameOf(v));
    }

    private static int asInt(Object v, String path)
    {
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long)    return Math.toIntExact((Long) v);
        if (v instanceof String)
        {
            try { return Integer.parseInt(((String) v).trim()); }
            catch (NumberFormatException nfe)
            {
                throw new IllegalArgumentException(path + " must be an integer; got '" + v + '\'');
            }
        }
        throw new IllegalArgumentException(path + " must be an integer; got " + classNameOf(v));
    }

    private static boolean asBool(Object v, String path)
    {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String)
        {
            String s = ((String) v).trim().toLowerCase();
            if ("true".equals(s) || "yes".equals(s)) return true;
            if ("false".equals(s) || "no".equals(s)) return false;
        }
        throw new IllegalArgumentException(path + " must be a boolean; got " + classNameOf(v));
    }

    private static String classNameOf(Object v)
    {
        return v == null ? "null" : v.getClass().getSimpleName();
    }

    /** Fails fast on unknown keys so a typo'd field surfaces immediately. */
    private static void rejectUnknownKeys(Map<String, Object> m, String section, String... allowed)
    {
        Set<String> ok = new HashSet<>(Arrays.asList(allowed));
        for (String k : m.keySet())
        {
            if (!ok.contains(k))
                throw new IllegalArgumentException("Unknown key '" + k + "' under " + section
                                                   + ". Allowed: " + Arrays.toString(allowed));
        }
    }

    /** Suppresses unused warnings for the SnakeYAML imports; a future refactor may re-introduce them. */
    @SuppressWarnings("unused")
    private static void touchImports()
    {
        new Constructor(RunConfig.class, new LoaderOptions());
        BeanAccess access = BeanAccess.FIELD;
        Tag t = Tag.MAP;
    }
}
