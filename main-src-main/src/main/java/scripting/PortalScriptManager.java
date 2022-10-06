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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import client.MapleClient;
import constants.GameConstants;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptException;
import server.MaplePortal;
import tools.FileoutputUtil;
import tools.StringUtil;

public class PortalScriptManager {

    private static final PortalScriptManager instance = new PortalScriptManager();
    private final Map<String, PortalScript> scripts = new HashMap<>();
    private final static ScriptEngineFactory sef = new ScriptEngineManager().getEngineByName("nashorn").getFactory();

    public static PortalScriptManager getInstance() {
        return instance;
    }

    private PortalScript getPortalScript(final String scriptName) {
        if (scripts.containsKey(scriptName)) {
            return scripts.get(scriptName);
        }

        final File scriptFile = new File("腳本/傳送點/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            return null;
        }

        final ScriptEngine portal = sef.getScriptEngine();
        try (Stream<String> stream = Files.lines(scriptFile.toPath(),
                Charset.forName(StringUtil.codeString(scriptFile)))) {
            String lines = "load('nashorn:mozilla_compat.js');" //+ "load('classpath:net/arnx/nashorn/lib/promise.js');"
                    + stream.collect(Collectors.joining(System.lineSeparator()));
            CompiledScript compiled = ((Compilable) portal).compile(lines);
            compiled.eval();
            stream.close();
        } catch (ScriptException | IOException e) {
            System.err.println("讀取傳送點腳本出錯: " + scriptName + ":" + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "讀取傳送點腳本出錯 (" + scriptName + ") " + e);
        }
        final PortalScript script = ((Invocable) portal).getInterface(PortalScript.class);
        scripts.put(scriptName, script);
        return script;
    }

    public final void executePortalScript(final MaplePortal portal, final MapleClient c) {
        final PortalScript script = getPortalScript(portal.getScriptName());

        boolean err = false;
        if (script != null) {
            try {
                if (c.getPlayer().isShowInfo()) {
                    c.getPlayer().showInfo("傳送點腳本", false,
                            "執行傳送點腳本:" + portal.getScriptName() + ".js" + c.getPlayer().getMap());
                }
                script.enter(new PortalPlayerInteraction(c, portal));
            } catch (Exception e) {
                err = true;
                System.err.println("執行傳送點腳本出錯: " + portal.getScriptName() + " : " + e);
            }
        } else {
            err = true;
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("傳送點腳本", true,
                        "未找到處理傳送點(" + portal.getScriptName() + ")的腳本" + c.getPlayer().getMap());
            }
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log,
                    "\r\n\r\n未找到處理傳送點(" + portal.getScriptName() + ")的腳本" + c.getPlayer().getMap() + "\r\n\r\n");
        }
        if (err && !GameConstants.isTutorialMap(c.getPlayer().getMapId())) {
            c.getPlayer().卡圖 = c.getPlayer().getMapId();
            c.getPlayer().dropMessage(5, "你好像被卡在了奇怪的地方，這裡有個東西未處理，請聯繫管理員反饋訊息：" + portal.getScriptName()
                    + "\r\n你現在可以點擊 拍賣 或者輸入 @卡圖 來移動到弓箭手村。");
        }
    }

    public final void clearScripts() {
        scripts.clear();
    }
}
