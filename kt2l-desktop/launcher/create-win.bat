@REM
@REM kt2l-desktop - kt2l desktop app
@REM Copyright © 2024 Mike Hummel (mh@mhus.de)
@REM
@REM This program is free software: you can redistribute it and/or modify
@REM it under the terms of the GNU General Public License as published by
@REM the Free Software Foundation, either version 3 of the License, or
@REM (at your option) any later version.
@REM
@REM This program is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
@REM GNU General Public License for more details.
@REM
@REM You should have received a copy of the GNU General Public License
@REM along with this program. If not, see <http://www.gnu.org/licenses/>.
@REM


cd kt2l-desktop\target

jpackage  --name KT2L ^
  --input . ^
  --main-jar kt2l-desktop-0.0.1-SNAPSHOT.jar ^
  --type exe ^
  --java-options "-XstartOnFirstThread -Dspring.profiles.active=prod" ^
  --vendor "www.kt2l.org"

copy KT2L-1.0.exe KT2L.exe

