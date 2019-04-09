/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.nowtoapps.bluevpn.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nowtoapps.bluevpn.Core.app
import com.nowtoapps.bluevpn.utils.Key

@Database(entities = [Profile::class, KeyValuePair::class], version = 1)
abstract class PrivateDatabase : RoomDatabase() {
    companion object {
        private val instance by lazy {
            Room.databaseBuilder(app, PrivateDatabase::class.java, Key.DB_PROFILE)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }

        val profileDao get() = instance.profileDao()
        val kvPairDao get() = instance.keyValuePairDao()
    }

    abstract fun profileDao(): Profile.Dao
    abstract fun keyValuePairDao(): KeyValuePair.Dao
}
