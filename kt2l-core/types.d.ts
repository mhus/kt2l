///
/// kt2l-core - kt2l core implementation
/// Copyright © 2024 Mike Hummel (mh@mhus.de)
///
/// This program is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// This program is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with this program. If not, see <http://www.gnu.org/licenses/>.
///

// This TypeScript modules definition file is generated by vaadin-maven-plugin.
// You can not directly import your different static files into TypeScript,
// This is needed for TypeScript compiler to declare and export as a TypeScript module.
// It is recommended to commit this file to the VCS.
// You might want to change the configurations to fit your preferences
declare module '*.css?inline' {
  import type { CSSResultGroup } from 'lit';
  const content: CSSResultGroup;
  export default content;
}

// Allow any CSS Custom Properties
declare module 'csstype' {
  interface Properties {
    [index: `--${string}`]: any;
  }
}
