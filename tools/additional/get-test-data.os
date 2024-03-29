
#Использовать v8runner
#Использовать cmdline
#Использовать tempfiles
#Использовать logos

Перем Лог;
Перем ВерсияПлатформы;

Процедура ВыгрузитьБазу(Знач Откуда, Знач КудаПуть, Знач Пользователь = Неопределено, Знач Пароль = Неопределено)
	
	Лог.Информация("Выгружаю эталон базы");
	Источник = Новый УправлениеКонфигуратором;
	Источник.УстановитьКонтекст(Откуда, Пользователь, Пароль);
	Если ЗначениеЗаполнено(ВерсияПлатформы) Тогда
		Источник.ИспользоватьВерсиюПлатформы(ВерсияПлатформы);
	КонецЕсли;
	Источник.ВыгрузитьИнформационнуюБазу(КудаПуть);

КонецПроцедуры

Процедура ЗагрузитьБазу(Знач ФайлВыгрузки, Знач Куда, Знач Пользователь = Неопределено, Знач Пароль = Неопределено, Знач КлючСеансов = Неопределено) Экспорт
	
	Лог.Информация("Загружаю в базу для тестирования");
	Приемник = Новый УправлениеКонфигуратором;
	Приемник.КаталогСборки(ТекущийКаталог());
	Приемник.УстановитьКонтекст(Куда,Пользователь,Пароль);
	Если ЗначениеЗаполнено(ВерсияПлатформы) Тогда
		Приемник.ИспользоватьВерсиюПлатформы(ВерсияПлатформы);
	КонецЕсли;

	Если ЗначениеЗаполнено(КлючСеансов) Тогда
		Приемник.УстановитьКлючРазрешенияЗапуска(КлючСеансов);
	КонецЕсли;

	Приемник.ЗагрузитьИнформационнуюБазу(ФайлВыгрузки);

КонецПроцедуры

Функция НастроитьПарсер()
	
	Парсер = Новый ПарсерАргументовКоманднойСтроки();
	Парсер.ДобавитьПараметр("Откуда", "База эталон");
	Парсер.ДобавитьПараметр("Куда", "База куда грузим");
	Парсер.ДобавитьИменованныйПараметр("-user", "Пользователь эталона");
	Парсер.ДобавитьИменованныйПараметр("-pwd", "Пароль эталона");
	Парсер.ДобавитьИменованныйПараметр("-destuser", "Пользователь приемника");
	Парсер.ДобавитьИменованныйПараметр("-destpwd", "Пароль приемника");
	Парсер.ДобавитьИменованныйПараметр("-version", "Версия платформы");
	Парсер.ДобавитьИменованныйПараметр("-lockuccode", "Код разрешения");

	Возврат Парсер;

КонецФункции

Лог = Логирование.ПолучитьЛог("oscript.get-test-data");

Парсер = НастроитьПарсер();
ВведенныеПараметры = Парсер.Разобрать(АргументыКоманднойСтроки);
ВерсияПлатформы = ВведенныеПараметры["-version"];

Если Не ЗначениеЗаполнено(ВведенныеПараметры["Откуда"]) Тогда
	Парсер.ВывестиСправкуПоПараметрам();
	ЗавершитьРаботу(1);
КонецЕсли;

ВременнаяВыгрузка = ВременныеФайлы.НовоеИмяФайла("dt");
Попытка
	ВыгрузитьБазу(ВведенныеПараметры["Откуда"], ВременнаяВыгрузка, ВведенныеПараметры["-user"], ВведенныеПараметры["-password"]);
	Попытка
		Лог.Информация("Загружаю без пароля");
		ЗагрузитьБазу(ВременнаяВыгрузка, ВведенныеПараметры["Куда"],,, ВведенныеПараметры["-lockuccode"]);
	Исключение
		Лог.Ошибка(ИнформацияОбОшибке().Описание);
		Лог.Информация("Пробуем загрузить с паролем");
		ЗагрузитьБазу(ВременнаяВыгрузка, ВведенныеПараметры["Куда"], ВведенныеПараметры["-destuser"], ВведенныеПараметры["-destpwd"], ВведенныеПараметры["-lockuccode"]);
	КонецПопытки;
Исключение
	ВременныеФайлы.Удалить();
	ВызватьИсключение;
КонецПопытки;

ВременныеФайлы.Удалить();
