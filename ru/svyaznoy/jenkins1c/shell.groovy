package ru.svyaznoy.jenkins1c
import ru.svyaznoy.jenkins1c.shell

class shell {

    String batEncoding = '65001'

    String getScript(def command) {
    
        def prefix = """@echo off
chcp $batEncoding > nul\r\n"""
        return prefix + command
    
    }

}