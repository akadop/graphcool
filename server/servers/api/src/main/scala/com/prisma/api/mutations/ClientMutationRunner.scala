package com.prisma.api.mutations

import com.prisma.api.ApiMetrics
import com.prisma.api.connector.DatabaseMutactionExecutor
import com.prisma.api.database.mutactions._
import com.prisma.api.mutactions.{DatabaseMutactionVerifier, SideEffectMutactionExecutor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ClientMutationRunner {

  def run[T](
      clientMutation: ClientMutation[T],
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor,
      databaseMutactionVerifier: DatabaseMutactionVerifier
  ): Future[T] = {
    for {
      preparedMutactions <- clientMutation.prepareMutactions()
      errors             = databaseMutactionVerifier.verify(preparedMutactions.databaseMutactions)
      _                  = if (errors.nonEmpty) throw errors.head
      _                  <- performMutactions(preparedMutactions, clientMutation.projectId, databaseMutactionExecutor, sideEffectMutactionExecutor)
      dataItem           <- clientMutation.getReturnValue
    } yield dataItem
  }

  private def performMutactions(
      preparedMutactions: PreparedMutactions,
      projectId: String,
      databaseMutactionExecutor: DatabaseMutactionExecutor,
      sideEffectMutactionExecutor: SideEffectMutactionExecutor
  ): Future[Unit] = {
    for {
      _ <- databaseMutactionExecutor.execute(preparedMutactions.databaseMutactions)
      _ <- sideEffectMutactionExecutor.execute(preparedMutactions.sideEffectMutactions)
    } yield ()
  }
}
