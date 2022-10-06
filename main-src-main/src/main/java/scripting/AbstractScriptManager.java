/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import java.io.File;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import client.MapleClient;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import tools.FileoutputUtil;
import tools.StringUtil;

/**
 *
 * @author Matze
 */
public abstract class AbstractScriptManager {

    private final ScriptEngineManager sem = new ScriptEngineManager();

    protected Invocable getInvocable(String path, MapleClient c) {
        return getInvocable(path, c, false);
    }

    protected Invocable getInvocable(String path, MapleClient c, boolean npc) {
        path = "腳本/" + path;
        ScriptEngine engine = null;

        if (c != null) {
            engine = c.getScriptEngine(path);
        }
        if (engine == null) {
            File scriptFile = new File(path);
            if (!scriptFile.exists()) {
                return null;
            }
            engine = sem.getEngineByName("nashorn");
            if (c != null) {
                c.setScriptEngine(path, engine);
            }
            try (Stream<String> stream = Files.lines(scriptFile.toPath(),
                    Charset.forName(StringUtil.codeString(scriptFile)))) {
                String lines = "load('nashorn:mozilla_compat.js');"
//                        + "load('classpath:net/arnx/nashorn/lib/promise.js');"
                        + stream.collect(Collectors.joining(System.lineSeparator()));
                engine.eval(lines);
                stream.close();
            } catch (ScriptException | IOException e) {
                System.err.println("Error executing script. Path: " + path + "\r\nException " + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log,
                        "Error executing script. Path: " + path + "\r\nException " + e);
                return null;
            }
        } else if (c != null && npc) {
            // c.getPlayer().dropMessage(-1, "You already are talking to this NPC. Use @ea
            // if this is not intended.");
        }
        return (Invocable) engine;
    }
}
